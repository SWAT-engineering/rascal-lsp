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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import engineering.swat.rascal.lsp.model.Language;
import engineering.swat.rascal.lsp.model.LanguageRegistry;
import io.usethesource.vallang.IValueFactory;

public class LSPServer {

	private final IValueFactory vf;
	private final LanguageRegistry registry;
	
	public LSPServer(IValueFactory vf) {
		this.vf = vf;
		this.registry = new LanguageRegistry();
	}

    private class ActualServer implements LanguageServer, LanguageClientAware {

        private final MultipleLanguageTextService mainService;
        
        public ActualServer() {
        	mainService = new MultipleLanguageTextService(registry);
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
        	throw new StopRunning();
        }

        @Override
        public TextDocumentService getTextDocumentService() {
            return mainService;
        }

		@Override
		public WorkspaceService getWorkspaceService() {
			return null;
		}

		@Override
		public void connect(LanguageClient client) {
			mainService.connect(client);
		}
    }
    
    @SuppressWarnings("serial")
	private static final class StopRunning extends RuntimeException {
    }
    
    private final AtomicBoolean shouldServerRun = new AtomicBoolean(false);
    private final Collection<Closeable> openSockets = new ConcurrentLinkedQueue<>();
    
    public void start(int port, String host, boolean asServer, boolean webSocket) throws InterruptedException, ExecutionException {
    	if (shouldServerRun.compareAndSet(false, true)) {
    		if (asServer) {
    			// we start a TCP server where multiple clients can connect to, and they all get their own instance of the ActualServer
    			Collection<SimpleEntry<Closeable, Future<Void>>> runningClients = new ConcurrentLinkedQueue<>();
    			AtomicBoolean isRunning = new AtomicBoolean(true);
    			startDaemonThread("Rascal LSP Server: " + host + ":" + port, () -> {
    				try (ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getByName(host))) {
    					openSockets.add(serverSocket);
                        Socket client;
                        while ((client = serverSocket.accept()) != null) {
                            openSockets.add(client);
							runningClients.add(new SimpleEntry<Closeable, Future<Void>>(client, constructLSPClient(client).startListening()));
                        }
    				} catch (UnknownHostException e1) {
    					throw new RuntimeException(e1);
    				} catch (IOException e1) {
    					;
    				}
    				finally {
    					isRunning.set(false);
    				}
    			});

    			startDaemonThread("Rascal LSP Server client cleanup: " + host + ":" + port, () -> {
    				while (isRunning.get()) {
    					try {
    						Thread.sleep(1000);
    					} catch (InterruptedException e) {
    						return;
    					}
    					Iterator<SimpleEntry<Closeable, Future<Void>>> it = runningClients.iterator();
    					while (it.hasNext()) {
    						SimpleEntry<Closeable, Future<Void>> c = it.next();
    						if (c.getValue().isDone()) {
    							try {
    								c.getKey().close();
    							} catch (Throwable to) { }
    							openSockets.remove(c.getKey());
    							it.remove();
    						}
    					}
    				}
    			});
    		}
    		else {
    			startDaemonThread("Rascal LSP Server listener for connections available on: " + host + ":" + port, () -> {
    				while (shouldServerRun.get()) {
    					try (Socket reverseServer = new Socket(host, port)) {
    						openSockets.add(reverseServer);
    						constructLSPClient(reverseServer).startListening().get();
    					}
    					catch (InterruptedException | ExecutionException | IOException e) {
    						openSockets.clear();
    						try {
    							Thread.sleep(1000);
    						} catch (InterruptedException e1) {
    							return;
    						}
    					}
    				}
    			});
    		}
    	}
    }

	private Launcher<LanguageClient> constructLSPClient(Socket client) throws IOException {
		ActualServer localService = new ActualServer();
		Launcher<LanguageClient> clientLauncher = new Builder<LanguageClient>()
		        .setLocalService(localService)
		        .setRemoteInterface(LanguageClient.class)
		        .setInput(client.getInputStream())
		        .setOutput(client.getOutputStream())
		        .create();
		localService.connect(clientLauncher.getRemoteProxy());
		return clientLauncher;
	}
    
    private static void startDaemonThread(String name, Runnable target) {
    	Thread t = new Thread(target);
    	t.setDaemon(true);
    	t.setName(name);
    	t.start();
    }

//    public static void main(String[] args) throws InterruptedException, ExecutionException, URISyntaxException {
//    	final Evaluator eval = ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(System.out), new PrintWriter(System.err));
//    	//eval.addRascalSearchPath(URIUtil.createFileLocation("c:/Users/Davy/swat.engineering/rascal/rascal-lsp/lsp/src/main/rascal/"));
//        //eval.addRascalSearchPath(URIUtil.getChildLocation(URIUtil.rootLocation("manifest"), "src/main/rascal")); 
//        eval.addRascalSearchPath(URIUtil.getChildLocation(URIUtil.rootLocation("manifest"), "/")); 
//        eval.doImport(null, "demo::lang::Syntax");
//        IValueFactory vf = ValueFactoryFactory.getValueFactory();
//    	new LSPServer(vf).start((s, l) -> 
//    		CompletableFuture.supplyAsync(() -> {
//    			synchronized (eval) {
//    				try {
//    					return (ISet)eval.call("getUseDef", vf.string(s), l);
//    				} catch (CancellationException c) {
//    					eval.interrupt();
//    					throw c;
//    				}
//				}
//    		})
//    	);
//	}

	public void register(String languageName, String extension, RascalBridge languageImplementation) {
		registry.put(extension, new Language(languageName, languageImplementation));
	}

	public void stop() {
		shouldServerRun.set(false);
		
		for (Closeable c : openSockets) {
			try {
				c.close();
			} catch (IOException e) {
			}
		}
	}
}
