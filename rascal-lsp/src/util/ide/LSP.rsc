@license{
Copyright (c) 2018, Davy Landman, SWAT.engineering
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
}
module util::ide::LSP

import ParseTree;
import Message;
import util::Reflective; 

data LSPSummary(
    rel[loc use, loc def] definition = {},
    rel[loc use, loc typeDef] typeDefinition = {}, 
    rel[loc use, loc implementation] implementation = {},

    // a flat list of all symbols in the file, hierarchy is not supported
    lrel[loc symbol, SymbolInformation info] documentSymbol = [],

    // (error) messages.
    set[Message] diagnostics = {},

    // documentation
    rel[loc at, loc url] documentLink = {}, // actual hyperlinks in the source code
    rel[loc at, str signature] signature = {}
) = file(loc f, datetime calculatedOn);

data Message(
    str source = "",  // which tool/phase this error message originated from
    lrel[loc other, str message] relatedInformation = [] // which other locations are also related to this message (for example with double declarations)
);

// workaround for ambiguity
alias PT[&T <: Tree] = &T <: Tree;

data LSPContext
    = context(
        LSPSummary currentSummary,
        PathConfig pathConfig,
        PT[&T <: Tree] (type[&T<:Tree] begin, loc l) getParseTree, // can also be used to get the parse tree of a different registered language
        void (set[Message] msg) reportMore
    );

// every language has it's own port, lsp does not allow multiplexing over the same server connection
// we do offer sharing between all languages registered within the same execution context (evaluator)
loc calculateLSPHost(str host, int port) = |lsp://<host>:<"<port>">|;

// start an LSP instance in the background.
// if asServer is false, it assumes a VS Code like communication style, where we have to be a tcp client instead of server
//@javaClass{engineering.swat.rascal.lsp.LSPServerRegistry}
//java void startLSP(loc lspServer, bool asServer = true, bool websocket = false);

@javaClass{engineering.swat.rascal.lsp.LSPServerRegistry}
java void stopLSP(loc lspServer);

// start a new LSP server, or update an existing one (combination of lspServer and languageName)
@javaClass{engineering.swat.rascal.lsp.LSPServerRegistry}
@reflect
java void registerLanguage(loc lspServer, str languageName, str extension,
    type[&T <: Tree] grammar, 
    LSPSummary (&T <: Tree tree, LSPContext ctx) calculateSummary, 
    set[LSPCapability[&T <: Tree]] capabilities, 
    PathConfig pcfg, bool webSocket = false, bool allowAmbiguity = false);

// functions can throw this exception to report failures in calculating the requested option.
// normal errors (like type checking errors) should show up in LSPSummary::diagnostic
data LSPException = lspError(set[Message] msgs);
      
data LSPCapability[&T <: Tree] 
    = definition()
    | typeDefinition()
    | implementation()
    | documentLink() // can locate actual hyperlinks in the document
    | diagnostics()

    | documentSymbol(set[SymbolKind] kinds)
    | signatureHelp(set[MarkupKind] formats) // LSPSummary::signature
    | references(list[loc] (&T <: Tree tree, loc cursor, LSPContext ctx) findReferences)
    | formatting(list[TextEdit] (&T <: Tree tree, LSPContext ctx) formatDocument) // aka: pretty printing
    | rangeFormatting(list[TextEdit] (&T <: Tree tree, loc range, LSPContext ctx) formatRange) // aka: pretty printing
    | rename(WorkSpaceEdit (&T <: Tree tree, loc cursor, str newName, LSPContext ctx) rename)
    | documentHighlight(rel[loc, HighlightKind] (&T <: Tree tree, loc cursor, LSPContext ctx) highlighter)  // highlight certain words
    // part of LSP, not yet supported:
    //| completion(_)
    //| hover(_)
    //| onTypeFormattting(_) : might be to chatty to format while typing
    //| codeAction(_)
    //| codeLens(_)
    //| documentColor() // annotates color literals with the color they represent (for color picker functionality)
    //| publishDiagnostics()
    // TODO: all workspace stuff
    ;
    
data MarkupKind 
    = markdown()
    | plaintext()
    ;

data TextEdit 
    // either a replacement or an insert.
    // an insert is when the begin and end column/line are the same
    = edit(loc at, str txt)
    ;

data WorkSpaceEdit
    = edits(lrel[loc file, list[TextEdit] edits] changes);
    
data HighlightKind
    = text()
    | read()
    | write()
    ;


data SymbolInformation
    = symbol(str name, SymbolKind kind, str container = "", bool deprecated = false);
    
data SymbolKind
    = file()
    | \module()
    | namespace()
    | package()
    | class()
    | method()
    | property()
    | field()
    | constructor()
    | enum()
    | interface()
    | function()
    | variable()
    | constant()
    | string()
    | number()
    | boolean()
    | array()
    | object()
    | key()
    | null()
    | enumMember()
    | struct()
    | event()
    | operator()
    | typeParameter()
    ;