function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (!cm.doingQuest()) { //able to accept a quest
            if (status == 0) {
                cm.sendSimple(cm.selectQuest(1337, "Randum text before selection")); // replace 1337 with ur questid
            } else if (status == 1) {
                cm.sendNext("blablabla");
            } else if (status == 2) {
                if (mode == 0) { //make the last status before accept decline like this
                    cm.sendOk("Oh dayam"); 
                    cm.dispose();
                } else {
                    cm.sendNextPrev("blablabla"); //go on like that till u come to the accept decline part
                }
            } else if (status == 3) {
                cm.sendAcceptDecline("Do you really want to accept my Quest:\r\n\r\n#e" + cm.getPlayer().getQuest().getQuestInfo());
            } else if (status == 4) {
                cm.sendPrev("Great...");
            } else if (status == 5) {
                cm.startQuest(1337); // Quest Id here...
                cm.getPlayer().dropYellow("Accepted: " + cm.getPlayer().getQuest().getQuestTitle());
                cm.dispose();
            }
        } else if (!cm.doingQuest(1337)) { // Quest Id here
            if (status == 0) {
                cm.sendYesNo("You already accepted: #r" + cm.getPlayer().getQuest().getQuestTitle());
            } else if (status == 1) {
                cm.sendOk("Quest Canceled!");
                cm.startQuest(0);
                cm.dispose();
            }
        } else if (cm.doingQuest(1337) && !cm.canComplete()) {
            if (status == 0) {
                if (mode == 0) {
                    cm.dispose();
                } else {
                    cm.sendSimple(cm.selectQuest(1337, "Randum text before selection"));
                }
            } else if (status == 1) {
                cm.sendYesNo("You didn't finish ma task, bro..."); //u can add the Quest Task as weel if u wanna ... cm.getPlayer().getQuest().getQuestInfo()
            } else if (status == 2) {
                cm.startQuest(0);
                cm.getPlayer().dropYellow("[Quest Canceled]");
                cm.dispose();
            } 
        } else if (cm.doingQuest(1337) && cm.canComplete()) {
            if (status == 0) {
                cm.sendSimple(1337, "Randum text before selection");
            } else if (status == 1) {
                cm.sendNext("blablablubb");
            } else if (status == 2) {
                cm.sendNextPrev("lol what lol wtf lol omg lol rofl lol lol roflcopter lol rofcopter lol i forgot the l lol ...");
            } else if (status == 3) { 
                cm.sendPrev(cm.rewardPlayer("Randum text before it shows ya tha fucking reward thing asdfasdfasdf"));
            } else if (status == 4) {
                cm.rewardPlayer(true, true);
                cm.getPlayer().dropYellow("[Quest Complete]");
                cm.dispose();
            }
        }
    }
}  