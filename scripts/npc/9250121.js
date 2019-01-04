var residue = 4000465;
var price = 5;
var server = "TropikMS";
var genderAsString;

function start() {
    genderAsString = cm.getPlayer().getGender() == 0 ? "Male" : "Female";
    cm.sendYesNo("#e#kYou are currently a #b"+genderAsString+"#k\r\n\r\nHello #r#h ##k,\r\nI am the gender swapper of #r"+server+"#k. You may swap your gender for the mere price of "+price+" coconuts.\r\n\r\nSon, let me warn you, i am not paid for this crap. I do it old school for thing such as cutting your penus, i might use the sharp edge of one of those can next to me.\r\n\r\nSo, are you interested?");
}

function action(mode, type, selection) {
    if (mode != 1) {
        cm.sendOk("#eI bet you're scared.");
        cm.dispose();
			} else {
		if (cm.haveItem(residue, price)) {
			if (cm.getPlayer().getGender() == 0) {
				cm.gainItem(residue, -price);
				cm.getPlayer().setGender(1);
				cm.sendOk("#eOH LOOK AT THE SIZE OF THAT THING! *cutcutcut*\r\nYou are now a #rFemale#k.");
				cm.dispose();
			} else if (cm.getPlayer().getGender() == 1) {
				cm.gainItem(residue, -price);
				cm.getPlayer().setGender(0);
				cm.sendOk("#eI have inserted the best of all the penus i have. You are now a #rMale.");
				cm.dispose();
			}
		} else {
			cm.sendOk("#eMan, i don't do shit for free. Go get some #bcoconuts#k then come talk to me.");
			cm.dispose();
        }
    }
}