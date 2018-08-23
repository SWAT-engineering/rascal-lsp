module demo::lang::Syntax

import ParseTree;
extend lang::std::Layout;
import Message;
import util::Reflective;
import DateTime;
import util::ide::LSP;
import IO;

lexical Id = [a-zA-Z]+ !>> [a-zA-Z];

start syntax Words = Id* ids;

rel[loc, loc] getUseDef(start[Words] p) {
    result = {};
    defined = ();
    for (Id i <- p.top.ids) {
        str name = "<i>";
        if (name notin defined) {
            defined[name] = i@\loc;
        } 
        else {
            result += <i@\loc, defined[name]>;
        }
    }
    return result;
}

set[Message] getWarnings(start[Words] p) 
    = { warning("Check if not an should be used", x@\loc, source = "grammar checker") | /x:(Id)`a` := p}
    + { info("Rascal rocks", r@\loc, source = "Marketing tool") | /r:(Id)`Rascal` := p }
    ;


loc srv = calculateLSPHost("localhost", 9000);

void updateRegistration() {
    registerLanguage(srv, "Test language", "wdr", #start[Words],
        LSPSummary (start[Words] t, LSPContext ctx) {
            return file(t@\loc, now(), 
                    definition = getUseDef(t),
                    diagnostics = getWarnings(t)
                );
        }, 
        {definition()}, pathConfig());

}