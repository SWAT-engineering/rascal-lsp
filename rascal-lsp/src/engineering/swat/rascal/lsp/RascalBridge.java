/** 
 * Copyright (c) 2018, Davy Landman, SWAT.engineering BV
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package engineering.swat.rascal.lsp;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import org.rascalmpl.ast.KeywordFormal;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.result.AbstractFunction;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.NonTerminalType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.types.ReifiedType;
import org.rascalmpl.interpreter.utils.RuntimeExceptionFactory;
import org.rascalmpl.parser.gtd.IGTD;
import org.rascalmpl.parser.gtd.exception.ParseError;
import org.rascalmpl.parser.gtd.result.action.IActionExecutor;
import org.rascalmpl.parser.gtd.result.out.DefaultNodeFlattener;
import org.rascalmpl.parser.uptr.UPTRNodeFactory;
import org.rascalmpl.parser.uptr.action.RascalFunctionActionExecutor;
import org.rascalmpl.semantics.dynamic.Import;
import org.rascalmpl.values.uptr.ITree;
import org.rascalmpl.values.uptr.SymbolAdapter;
import engineering.swat.rascal.lsp.model.Summary;
import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public class RascalBridge {
	
	private static final RascalTypeFactory RTF = RascalTypeFactory.getInstance();
	private static final TypeFactory TF = TypeFactory.getInstance();
	
	private final IValueFactory vf;
	private final IEvaluatorContext eval;
	private final Type[] summaryCalculationType;
	private final ICallableValue calculateSummary;
	//private final IConstructor startNonTerminal;
	private final boolean allowAmbiguity;
	private final IConstructor pathConfig;
	private final Type grammarType;
	private final Type lspContextType;
	private final Type lspContextConstructorType;

	private final IGTD<IConstructor, ITree, ISourceLocation> parser;
	private final String rootName;
	private final ILSPContext lspServerContext;
	private final ICallableValue parserFunction;
	private final ICallableValue reportMoreFunction;

	public RascalBridge(IValue grammar, IValue calculateSummary, ISet capabilities, IConstructor pathConfig, IBool allowAmbiguity, IEvaluatorContext ctx, ILSPContext lspServerContext) {

		this.lspServerContext = lspServerContext;
		this.grammarType = getGrammarType(grammar, grammar.getType());
		//this.startNonTerminal = (IConstructor)grammar;

		this.pathConfig = pathConfig;
		this.allowAmbiguity = allowAmbiguity.getValue();
		this.calculateSummary = (ICallableValue)calculateSummary;
		this.eval = ctx;
		this.vf = ctx.getValueFactory();
		
		synchronized(eval) {
			IConstructor startSort = (IConstructor) ((IConstructor) grammar).get("symbol");
			parser = Import.getParser(eval.getEvaluator(), (ModuleEnvironment) eval.getCurrentEnvt().getRoot(), (IMap) ((IConstructor)grammar).get("definitions"), false);
			String name = "";
			if (SymbolAdapter.isStartSort(startSort)) {
				name = "start__";
				startSort = SymbolAdapter.getStart(startSort);
			}

			if (SymbolAdapter.isSort(startSort) || SymbolAdapter.isLex(startSort) || SymbolAdapter.isLayouts(startSort)) {
				name += SymbolAdapter.getName(startSort);
			}
			rootName = name;
            lspContextType = getLSPContextType(eval).instantiate(Collections.singletonMap(TF.parameterType("T"), grammarType));
            lspContextConstructorType = getLSPContextConstructorType(ctx).instantiate(Collections.singletonMap(TF.parameterType("T"), grammarType));
            summaryCalculationType = new Type[] { grammarType, lspContextType };
            parserFunction = buildGetParseTreeFunction(eval, grammarType, lspContextConstructorType, lspServerContext::getTree);
            reportMoreFunction = buildReportMoreFunction(eval, lspContextConstructorType, lspServerContext::report);
		}
		
	}
	
	private static Type getLSPContextType(IEvaluatorContext eval) {
		ModuleEnvironment coreModule = eval.getHeap().getModule("util::ide::LSP");
		if (coreModule != null) {
			return coreModule.getStore().lookupAbstractDataType("LSPContext");
		}
		throw new RuntimeException("Cannot find util::ide::LSP in heap");
	}
	
	private static Type getLSPContextConstructorType(IEvaluatorContext eval) {
		ModuleEnvironment coreModule = eval.getHeap().getModule("util::ide::LSP");
		if (coreModule != null) {
			Type adt = coreModule.getStore().lookupAbstractDataType("LSPContext");
			return coreModule.getStore().lookupConstructor(adt, "context").stream().findFirst().orElseThrow(() -> new RuntimeException("Missing context constructor "));
		}
		throw new RuntimeException("Cannot find util::ide::LSP in heap");
	}

	private static ICallableValue buildReportMoreFunction(IEvaluatorContext ctx, Type constructorType, Consumer<IConstructor> reportTarget) {
		return new AbstractFunction(ctx.getCurrentAST(), ctx.getEvaluator(), (FunctionType)constructorType.getFieldType("reportMore"), Collections.<KeywordFormal>emptyList(), false, ctx.getCurrentEnvt()) {

			@Override
			public boolean isStatic() {
				return false;
			}

			@Override
			public ICallableValue cloneInto(Environment env) {
				// this can not happen because the function is not present in an environment
				return null;
			}

			@Override
			public boolean isDefault() {
				return false;
			}

			public org.rascalmpl.interpreter.result.Result<IValue> call(Type[] argTypes, IValue[] argValues, java.util.Map<String,IValue> keyArgValues) {
				reportTarget.accept((IConstructor) argValues[0]);
				return ResultFactory.nothing();
			};
		};
	}
	
	@FunctionalInterface
	public interface CheckedFunction<T, R, E extends Throwable> {
	   R apply(T t) throws E;
	}

	private static ICallableValue buildGetParseTreeFunction(IEvaluatorContext ctx, Type grammarType,Type constructorType, CheckedFunction<ISourceLocation, ITree, IOException> parser) {
		return new AbstractFunction(ctx.getCurrentAST(), ctx.getEvaluator(), (FunctionType)constructorType.getFieldType("getParseTree"), Collections.<KeywordFormal>emptyList(), false, ctx.getCurrentEnvt()) {

			@Override
			public boolean isStatic() {
				return false;
			}

			@Override
			public ICallableValue cloneInto(Environment env) {
				// this can not happen because the function is not present in an environment
				return null;
			}

			@Override
			public boolean isDefault() {
				return false;
			}

			public org.rascalmpl.interpreter.result.Result<IValue> call(Type[] argTypes, IValue[] argValues, java.util.Map<String,IValue> keyArgValues) {
				try {
					return ResultFactory.makeResult(grammarType, parser.apply((ISourceLocation) argValues[1]), ctx);
				}
				catch (ParseError pe) {
					ISourceLocation errorLoc = vf.sourceLocation(vf.sourceLocation(pe.getLocation()), pe.getOffset(), pe.getLength(), pe.getBeginLine(), pe.getEndLine(), pe.getBeginColumn(), pe.getEndColumn());
					throw RuntimeExceptionFactory.parseError(errorLoc, ctx.getCurrentAST(), ctx.getStackTrace());
				} catch (IOException e) {
					throw RuntimeExceptionFactory.io(vf.string(e.getMessage()), ctx.getCurrentAST(), ctx.getStackTrace());
				}
			};
		};
	}

	
	public IConstructor buildEmptySummary(ISourceLocation file) {
		return vf.constructor(lspContextConstructorType, file, vf.datetime(0));
	}

	public Summary calculateSummary(ITree tree, Summary previousSummary) {
		IValue context = vf.constructor(lspContextConstructorType, previousSummary.getRascalSummary(), pathConfig, parserFunction, reportMoreFunction);
		synchronized (eval) {
			Result<IValue> result = calculateSummary.call(summaryCalculationType, new IValue[] { tree, context }, Collections.emptyMap());
			if (result != null) {
				return new Summary((IConstructor) result.getValue());
			}
			return null;
		}
	}
	
	public ITree parse(char[] input, ISourceLocation loc) throws ParseError {
			// TODO: for the future, we could decide not to have parser amb/prod nodes call back into rascal (semantic actions in the parser), so we can avoid the lock.
//			IActionExecutor<ITree> exec = new RascalFunctionActionExecutor(eval, true);
			return (ITree) parser.parse(rootName, loc.getURI(), input, /*exec, */new DefaultNodeFlattener<IConstructor, ITree, ISourceLocation>(), new UPTRNodeFactory(allowAmbiguity));
		//}
	}
	
	private static Type getGrammarType(IValue start, Type reified) {
		if (!(reified instanceof ReifiedType)) {
		   throw RuntimeExceptionFactory.illegalArgument(start, null, null, "A reified type is required instead of " + reified);
		}
		Type grammarType = reified.getTypeParameters().getFieldType(0);
		if (!(grammarType instanceof NonTerminalType)) {
			throw RuntimeExceptionFactory.illegalArgument(start, null, null, "A non-terminal type is required instead of  " + grammarType);
		}
		return grammarType;
	}
}
