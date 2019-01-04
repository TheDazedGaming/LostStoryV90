    function start() {
            cm.sendSimple("#r#eDouble EXP and Mesos Card#k, #bDOUBLE THE FUN#k!\r\n Hey, I'm the double card seller, i sell double EXP and double Mesos card for those who want to level up and earn money faster. Let me warn you though that it won't be free nor easy to get these. \r\n\r\n #L0##v5211046# Double EXP#l\r\n\r\n            #v4000465# x3500 \r\n            500 Reborns\r\n\r\n #L1##v5360042# Double Mesos#l\r\n\r\n           #v4000465# x7000");
    }
     
    function action(m, t, s) {
        cm.dispose();
        if (m > 0 && s > -1 && s < 2) {
            if (s == 0 ? (cm.haveItem(4000465, 3500) && cm.getPlayer().getReborns() >= 500) : cm.haveItem(4000465, 7000)) {
                cm.gainItem(s == 0 ? 5211046 : 5360042);
                cm.sendOk("#eEnjoy your Double "+ (s == 0 ? "Exp" : "Mesos") +"!");
            } else
                cm.sendOk("#eYou do not meet one of the requirement!");    
            }
    }
