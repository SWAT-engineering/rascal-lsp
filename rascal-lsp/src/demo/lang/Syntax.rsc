module demo::lang::Syntax

import ParseTree;
extend lang::std::Layout;

lexical Id = [a-zA-Z]+ !>> [a-zA-Z];

start syntax Words = Id* ids;

rel[loc, loc] getUseDef(str contents, loc origin) {
    p = parse(#start[Words], contents, origin);
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