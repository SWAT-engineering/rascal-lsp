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

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.rascalmpl.uri.URIUtil;
import engineering.swat.rascal.lsp.util.LocationToRangeMap;
import engineering.swat.rascal.lsp.util.TreeMapLookup;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;

public class DSLService implements TextDocumentService {
	
	private final IValueFactory vf;
	private final Map<ISourceLocation, String> fileContents;
	private final Map<ISourceLocation, CompletableFuture<LocationToRangeMap>> defineMap;
	private final BiFunction<String, ISourceLocation, CompletableFuture<ISet>> useDefCalculation;
	private final static CompletableFuture<LocationToRangeMap> emptyLocationRangeMap = CompletableFuture.completedFuture((l) -> null);

	public DSLService(IValueFactory vf, BiFunction<String, ISourceLocation, CompletableFuture<ISet>> useDefCalculation) {
		this.vf = vf;
		this.useDefCalculation = useDefCalculation;
		this.fileContents = new ConcurrentHashMap<>();
		this.defineMap = new ConcurrentHashMap<>();
	}

	public void setCapabilities(ServerCapabilities result) {
		result.setDefinitionProvider(true);
		result.setTextDocumentSync(TextDocumentSyncKind.Full);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		ISourceLocation loc = toLoc(params.getTextDocument());
		String text = params.getTextDocument().getText();
		fileContents.put(loc, text);
		processChanges(loc, text);
	}

	private static ISourceLocation toLoc(TextDocumentItem doc) {
		return toLoc(doc.getUri());
	}
	private static ISourceLocation toLoc(TextDocumentIdentifier doc) {
		return toLoc(doc.getUri());
	}

	private static ISourceLocation toLoc(String uri) {
		try {
			return URIUtil.createFromURI(uri);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		ISourceLocation loc = toLoc(params.getTextDocument());
		for (TextDocumentContentChangeEvent c: params.getContentChanges()) {
			fileContents.put(loc, c.getText());
		}
		processChanges(loc, fileContents.get(loc));
	}


	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		ISourceLocation loc = toLoc(params.getTextDocument());
		if (fileContents.remove(loc) == null) {
			throw new RuntimeException("closed file "+ loc + " was not openend");
		}
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		
	}
	
	private void processChanges(ISourceLocation loc, String contents) {
		defineMap.put(loc, useDefCalculation.apply(contents, loc).thenApplyAsync((useDefs) -> {
			TreeMapLookup result = new TreeMapLookup();
			if (useDefs != null) {
				for (IValue ent: useDefs) {
					if (ent instanceof ITuple) {
						ITuple tp = (ITuple)ent;
						result.add(toJSPLoc((ISourceLocation)tp.get(0)), toJSPLoc((ISourceLocation)tp.get(1)));
					}
				}
			}
			return (LocationToRangeMap)result;
		}));
	}
	
	private static Location toJSPLoc(ISourceLocation sloc) {
		return new Location(sloc.getURI().toString(), toRange(sloc));
	}
	private static Range toRange(ISourceLocation sloc) {
		return new Range(new Position(sloc.getBeginLine() - 1, sloc.getBeginColumn()), new Position(sloc.getEndLine() - 1, sloc.getEndColumn()));
	}

	private static Range toRange(Position pos) {
		return new Range(pos, pos);
	}

	@Override
	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
		ISourceLocation loc = toLoc(position.getTextDocument());
		return defineMap.getOrDefault(loc, emptyLocationRangeMap).thenApply((defines) -> {
			Location fullLoc = new Location(loc.getURI().toString(), toRange(position.getPosition()));
			Location result = defines.lookup(fullLoc);
			if (result == null) {
				return Collections.emptyList();
			}
			return Collections.singletonList(result);
		});
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
			CompletionParams position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
			TextDocumentPositionParams position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(
			DocumentSymbolParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
			DocumentRangeFormattingParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(
			DocumentOnTypeFormattingParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		// TODO Auto-generated method stub
		return null;
	}



}
