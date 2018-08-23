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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.utils.RuntimeExceptionFactory;
import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;

public class LSPServerRegistry {
	
	// enums are a much simpeler implementation of singletons in Java, same guarantees, without the hassle
	private static enum ServerRegistry {
		INSTANCE;
		
		private final ConcurrentMap<ISourceLocation, LSPServer> instances = new ConcurrentHashMap<>();

		public LSPServer getRunningServer(ISourceLocation loc) {
			return instances.get(loc);
		}

		public boolean addNewServer(ISourceLocation loc, LSPServer server) {
			return instances.putIfAbsent(loc, server) == null;
		}

		public void stopServer(ISourceLocation loc) {
			LSPServer removed = instances.remove(loc);
			if (removed != null) {
				removed.stop();
			}
		}
	}

	private IValueFactory vf;
	
	public LSPServerRegistry(IValueFactory vf) {
		this.vf = vf;
	}
	
	
	public void registerLanguage(ISourceLocation lspServer, IString languageName, IString extension,
			IValue grammar, IValue calculateSummary, ISet capabilities, 
			IConstructor pathConfig, IBool wrapWebSocket, IBool allowAmbiguity, IEvaluatorContext ctx) {
		LSPServer server = ServerRegistry.INSTANCE.getRunningServer(lspServer);
		if (server == null) {
			// try to start it
			server = new LSPServer();
			try {
				if (ServerRegistry.INSTANCE.addNewServer(lspServer, server)) {
					// if we were the first, we actually start the server
					server.start(lspServer.getURI().getPort(), lspServer.getURI().getHost(), wrapWebSocket.getValue());
				}
				else {
					server = ServerRegistry.INSTANCE.getRunningServer(lspServer); 
					assert server != null;
				}
			} catch (InterruptedException | ExecutionException e) {
				throw RuntimeExceptionFactory.io(vf.string("Cannot start server: " +e.toString()), null, null);
			}
		}
		server.register(languageName.getValue(), extension.getValue(), new RascalBridge(grammar, calculateSummary, capabilities, pathConfig, allowAmbiguity, ctx));
	}
	
	public void stopLSP(ISourceLocation lspServer) {
		ServerRegistry.INSTANCE.stopServer(lspServer);
	}
	
}
