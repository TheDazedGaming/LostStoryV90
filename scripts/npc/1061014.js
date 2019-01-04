var medalid = [1142085, 1142086, 1142087, 1142088, 1142089, 1142090, 1142091, 1142092, 1142093, 1142094, 1142095, 1142096, 1142097, 1142098, 1142099, 1142100];
var rebornprice = [1, 10, 20, 45, 70, 100, 150, 200, 260, 400, 600, 700, 800, 900, 1000];
var watk = [1, 3, 5, 7, 10, 14, 20, 25, 30, 40, 50, 65, 75, 85, 100];
var text;

function start() {
    text = "#eHello, i am the medal exchanger for TropikMS. You can earn medals that give weapon attack when you meet the amount of reborns required.\r\n";
    for (i = 0; i < 15; i++) {
            text += ""+i+". #L"+i+"##v"+medalid[i]+"# #r"+watk[i]+" watkk#k | #b"+rebornprice[i]+" Reborns#k required#l\r\n";
        }
        cm.sendSimple(text);
}

function action(m,t,s) {
    if (m != 1) {
        cm.sendOk("#eCome back at any time if you have enough #breborns#k!");
        cm.dispose();
    } else {
        if (s != 0) {
            if (cm.getPlayer().getReborns() >= rebornprice[s]) {
                cm.gainItem(medalid[s]);
                Packages.server.MapleInventoryManipulator.editEquipById(cm.getPlayer(), 1, medalid[s], "watk", watk[s]);
                cm.reloadChar();
                cm.sendOk("#eYou have recieved your \r\n\r\n #v"+medalid[s]+"# #r"+watk[s]+" watk #k| #b"+rebornprice[s]+" Reborns#k required");
                cm.dispose();
            } else {
                cm.sendOk("#eHey! You are missing one of the req!");
                cm.dispose();
            }
        } else {
            if (cm.getPlayer().getReborns() >= rebornprice[s]) {
                cm.gainItem(medalid[s]);
                Packages.server.MapleInventoryManipulator.editEquipById(cm.getPlayer(), 1, medalid[s], "watk", watk[s]);
                cm.reloadChar();
                cm.sendOk("#eYou have recieved your \r\n\r\n #v"+medalid[s]+"# #r"+watk[s]+" watk #k| #b"+rebornprice[s]+" Reborns#k required");
                cm.dispose();
            } else {
                cm.sendOk("#eHey! You are missing one of the req!");
                cm.dispose();
            }
        }
    }
}