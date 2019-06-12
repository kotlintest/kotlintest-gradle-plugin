function Console() {
    this.messages = [];
    this.log = function(msg) { messages.push(msg);};
}

var console = new Console();