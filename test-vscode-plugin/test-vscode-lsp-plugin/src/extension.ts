'use strict';
// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';
import * as net from 'net';

//import { workspace, ExtensionContext } from 'vscode';

import {
	LanguageClient, LanguageClientOptions, ServerOptions, StreamInfo, //, TransportKind
    Trace
} from 'vscode-languageclient';

function tryOpenConnection(port: number, host: string, maxTries: number, retryDelay: number): Thenable<net.Socket> {
    return new Promise((connected, failed) => {
        const client = new net.Socket();
        var tries = 0;
        function retry(err?: Error) {
            if (tries <= maxTries) {
                tries++;
                client.connect(port, host);
            }
            else {
                failed("Connection retries exceeded" + (err ? (": " + err.message) : ""));
            }
        }
        // normal error case, timeout of the connection setup
        client.setTimeout(retryDelay);
        client.on('timeout', retry);
        // random errors, also retry
        client.on('error', retry);
        // success, so let's report back
        client.once('connect', () => {
            client.setTimeout(0); // undo the timeout
            client.removeAllListeners(); // remove the error listener
            connected(client);
        });
        // kick-off the retry loop
        retry();
    });
}

export function activate(context: vscode.ExtensionContext) {

    // This line of code will only be executed once when your extension is activated

    // TODO: Start server exe and communicate with it
    // let serverExe = 'java';

    // let ServerOptions: ServerOptions = {
    //     run: {command: serverExe, args:['-jar', 'C:/Users/Davy/swat.engineering/rascal/rascal-lsp/rascal-lsp-server.jar']},
    //     debug: {command: serverExe, args:['-jar', 'C:/Users/Davy/swat.engineering/rascal/rascal-lsp/rascal-lsp-server.jar']}
    // };
    console.log("starting extension");
    registerTermTest(context);
    registerBird(context);
}

function registerTermTest(context: vscode.ExtensionContext) {
    const Server: ServerOptions = () => tryOpenConnection(9000, 'localhost', 10, 1000).then(s => <StreamInfo>{
        writer: s,
        reader: s
    });
    let clientOptions: LanguageClientOptions = {
        documentSelector: [
            {
                pattern: '**/*.wdr',
            }
        ],
    };
    let lspClient = new LanguageClient("Hello LSP", Server, clientOptions);
    // For debugging only
    lspClient.trace = Trace.Verbose;
    //add all disposables here
    context.subscriptions.push(lspClient.start());
}

function registerBird(context: vscode.ExtensionContext) {
    const Server: ServerOptions = () => tryOpenConnection(9101, 'localhost', 10, 1000).then(s => <StreamInfo>{
        writer: s,
        reader: s
    });
    let clientOptions: LanguageClientOptions = {
        documentSelector: [
            {
                pattern: '**/*.bird',
            }
        ],
    };
    let lspClient = new LanguageClient("Bird Language", Server, clientOptions);
    // For debugging only
    lspClient.trace = Trace.Verbose;
    //add all disposables here
    context.subscriptions.push(lspClient.start());
}

// this method is called when your extension is activated
// your extension is activated the very first time the command is executed
export function activate2(context: vscode.ExtensionContext) {

    // Use the console to output diagnostic information (console.log) and errors (console.error)
    // This line of code will only be executed once when your extension is activated
    console.log('Congratulations, your extension "test-vscode-lsp-plugin" is now active!');

    // The command has been defined in the package.json file
    // Now provide the implementation of the command with  registerCommand
    // The commandId parameter must match the command field in package.json
    let disposable = vscode.commands.registerCommand('extension.sayHello', () => {
        // The code you place here will be executed every time your command is executed

        // Display a message box to the user
        vscode.window.showInformationMessage('Hello World!');
    });

    context.subscriptions.push(disposable);
}

// this method is called when your extension is deactivated
export function deactivate() {
}