var mapid = [105040100, 105040200, 800010100];

function start() {
cm.sendSimple("#e#bHello, I am the most powerful being on earth so they made me responsible for the training center. So where are we training today? \r\n\r\n#k#L0#Skelegons Map 1 (Less than 20 rebirths only)#l\r\n#L1#Skelegons Map 2 (Less than 50 Rebirths only)#l\r\n#L2#Training map 3 (recommended Rebirth: 25+)#l");
}
function action(mode, type, selection) {
        if (mode == 1) {
            cm.warp(mapid[selection]);
            cm.dispose();
    }
  cm.dispose();
}