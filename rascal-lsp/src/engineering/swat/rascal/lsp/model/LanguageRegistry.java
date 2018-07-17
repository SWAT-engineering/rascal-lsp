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
package engineering.swat.rascal.lsp.model;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import io.usethesource.vallang.ISourceLocation;

public class LanguageRegistry {
	
	private final Semaphore languagesChanged = new Semaphore(0);
	private final Map<String, Language> languages = new ConcurrentHashMap<>();
	
	public void put(String extension, Language language) {
		languages.put(extension, language);
		languagesChanged.release();
	}
	
	public Optional<Language> get(ISourceLocation loc) {
		return Optional.ofNullable(languages.get(getExtension(loc)));
	}
	
	private static String getExtension(ISourceLocation loc) {
		String path = loc.getPath();
		int extensionIndex = path.lastIndexOf('.');
		if (extensionIndex <= 0) {
			throw new IllegalArgumentException("No extension found for: " + loc);
		}
		return path.substring(extensionIndex + 1);
	}
	
	public CompletableFuture<Language> getAsync(ISourceLocation loc) {
		String extension = getExtension(loc);
		return CompletableFuture.supplyAsync(() -> {
			Language result;
			while ((result = languages.get(extension)) == null) {
				try {
					languagesChanged.acquire();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return result;
		});
	}
}
