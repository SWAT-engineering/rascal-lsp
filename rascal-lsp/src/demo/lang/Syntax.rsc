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



loc srv = |lsp://localhost:9000|;

void init() {
    startLSP(srv, asServer=false);
}

void stop() {

}
void updateRegistration() {
    registerLanguage(srv, "Test language", "wdr", #start[Words],
        LSPSummary (start[Words] t, LSPContext[start[Words]] ctx) {
            return file(t@\loc, now(), definition = getUseDef(t));
        }, 
        {definition()}, pathConfig());

}


void testFunc(&T (&T <: Tree, &T <: Tree) mutate) {
    println("Run?");
}


void callFunc() {
    testFunc(Id (Id x, Id y) { return x; });
}
