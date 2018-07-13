package engineering.swat.rascal.lsp;

import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.values.ValueFactoryFactory;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValueFactory;

public class LSPServer {

	private IValueFactory vf;
	
	public LSPServer(IValueFactory vf) {
		this.vf = vf;
	}

    private class ActualServer implements LanguageServer {

        private final DSLService mainService;
        
        public ActualServer(BiFunction<String, ISourceLocation, CompletableFuture<ISet>> calc) {
        	mainService = new DSLService(vf, calc);
		}

		@Override
        public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
            ServerCapabilities result = new ServerCapabilities();
            mainService.setCapabilities(result);
            return CompletableFuture.completedFuture(new InitializeResult(result));
        }

        @Override
        public CompletableFuture<Object> shutdown() {
            return null;
        }

        @Override
        public void exit() {

        }

        @Override
        public TextDocumentService getTextDocumentService() {
            return mainService;
        }

		@Override
		public WorkspaceService getWorkspaceService() {
			return null;
		}
    }

    public void start(BiFunction<String, ISourceLocation, CompletableFuture<ISet>> calc) throws InterruptedException, ExecutionException {
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(new ActualServer(calc), System.in, System.out);
        launcher.startListening().get();
    }
    
    public static void main(String[] args) throws InterruptedException, ExecutionException, URISyntaxException {
    	final Evaluator eval = ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(System.out), new PrintWriter(System.err));
    	//eval.addRascalSearchPath(URIUtil.createFileLocation("c:/Users/Davy/swat.engineering/rascal/rascal-lsp/lsp/src/main/rascal/"));
        //eval.addRascalSearchPath(URIUtil.getChildLocation(URIUtil.rootLocation("manifest"), "src/main/rascal")); 
        eval.addRascalSearchPath(URIUtil.getChildLocation(URIUtil.rootLocation("manifest"), "/")); 
        eval.doImport(null, "demo::lang::Syntax");
        IValueFactory vf = ValueFactoryFactory.getValueFactory();
    	new LSPServer(vf).start((s, l) -> 
    		CompletableFuture.supplyAsync(() -> {
    			synchronized (eval) {
    				try {
    					return (ISet)eval.call("getUseDef", vf.string(s), l);
    				} catch (CancellationException c) {
    					eval.interrupt();
    					throw c;
    				}
				}
    		})
    	);
	}
}
