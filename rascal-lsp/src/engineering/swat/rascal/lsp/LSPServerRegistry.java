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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.utils.RuntimeExceptionFactory;
import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IInteger;
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
	
	public ISourceLocation guessLSPServerId(IInteger port, IString host) {
		try {
			return vf.sourceLocation("lsp", host.getValue() + ":" + port.intValue(), null);
		} catch (URISyntaxException e) {
			throw RuntimeExceptionFactory.io(vf.string("Invalid host or port: " +e.toString()), null, null);
		}
	}
	
	
	public void startLSP(ISourceLocation lspServer, IBool asServer, IBool wrapWebSocket) {
		try {
			LSPServer server = new LSPServer(vf);
			if (ServerRegistry.INSTANCE.addNewServer(lspServer, server)) {
				server.start(lspServer.getURI().getPort(), lspServer.getURI().getHost(), asServer.getValue(), wrapWebSocket.getValue()); // TODO: change interface
			}
		} catch (InterruptedException | ExecutionException e) {
			throw RuntimeExceptionFactory.io(vf.string("Cannot start server: " +e.toString()), null, null);
		}
	}
	
	public void registerLanguage(ISourceLocation lspServer, IString languageName, IString extension,
			IValue grammar, IValue calculateSummary, ISet capabilities, 
			IConstructor pathConfig, IBool allowAmbiguity, IEvaluatorContext ctx) {
		LSPServer server = ServerRegistry.INSTANCE.getRunningServer(lspServer);
		if (server == null) {
			// throw
			throw RuntimeExceptionFactory.io(vf.string("Non-existing LSP server, did you call startLSP?"), ctx.getCurrentAST(), ctx.getStackTrace());
		}
		server.register(languageName.getValue(), extension.getValue(), new RascalBridge(grammar, calculateSummary, capabilities, pathConfig, allowAmbiguity, ctx));
	}
	
	public void stopLSP(ISourceLocation lspServer) {
		ServerRegistry.INSTANCE.stopServer(lspServer);
	}
	
}
