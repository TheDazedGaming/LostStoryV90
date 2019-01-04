var status = -1;
var dualbladeid = [430, 431, 432, 433, 434, 500]; //6th id is for debug
var dualbladelevel = [10, 30, 50, 70, 100];
var evanid = [2200, 2210, 2211, 2212, 2213, 2214, 2215, 2216, 2217, 2218];
//var evanlevel = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]; //Stupidity at its best. I forgot the dragon grow by itself...
var isEvan = false;
var isDB = false;

function start() {
    action(1,0,0);
}

function action(m,t,s) {
    status++;
    if (m != 1) {
        cm.dispose();
        return;
    }
    if (cm.getPlayer().getJob() == 0) {
        noobAdvance(m,t,s);
    } else {
        for(i = 0; i < evanid.length; i++) {
            if (cm.getPlayer().getJob() == evanid[i]) {
                isEvan = true;
                isDB = false;
                break;
            } else {
                continue;
            }
        }  
        isDB = isEvan == true ? false : true;
        if (isDB == false) {
            evanAdvance(m,t,s);
        }
        if (isEvan == false) {
            DualBladeAdvance(m,t,s);
        }
    }
}

function evanAdvance(m,t,s) {
    cm.sendOk("#eYou don't need to come and see me! Your dragon evolve by itself when you level up.");
    cm.dispose();
}

function DualBladeAdvance(m,t,s) {
    if (cm.getPlayer().getJob() == 434) {
        cm.sendOk("#eYou have already reached the highest job advancement for dual blade!");
        cm.dispose();
    } else {
        for(i = 0; i < dualbladeid.length; i++) {
            if (cm.getPlayer().getJob() == dualbladeid[i]) {
                break;
            }
        }
        if (status == 0) {
            if (cm.getPlayer().getLevel() >= dualbladelevel[i]) {
                cm.sendYesNo("#eGood job on reaching level #r"+cm.getPlayer().getLevel()+"#k. Would you like to advance to #r"+cm.getJobName(dualbladeid[i + 1])+"#k ?");
            } else {
                cm.sendOk("#eYou need to reach level #r"+dualbladelevel[i]+"#k to get your next job advancement.");
                cm.dispose();
            }
        } else if (status == 1) {
            cm.changeJobById(dualbladeid[i+1]);
            if (cm.getPlayer().getLevel() >= dualbladelevel[i+1]) {
                DualBladeAdvance(m,t,s);
            } else {
                cm.dispose();
            }
        }
    }
}

function noobAdvance(m,t,s) {
    if (status == 0) {
        cm.sendSimple("#eHello #h #! Would you like to be one of those 2 new jobs?\r\n\r\n#r#L0#Dual Blade#l\r\n#L1#Evan#l");
    } else if (status != 0) {
        if (s == 0) {
            cm.changeJobById(430);
            if (cm.getLevel() >= 30) {
                DualBladeAdvance(m,t,s);
            } else {
            cm.dispose();
            }
        } else if (s == 1) {
            cm.changeJobById(2200);
            cm.dispose();
        }
    }
}