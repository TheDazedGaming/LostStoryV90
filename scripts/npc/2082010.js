var onehandedsword = [1302007, 1302002, 1302008, 1302009, 1302010, 1302011, 1302012, 1302018, 1302023, 1302056, 1302059];
var twohandedsword = [1402001, 1402000, 1402002, 1402007, 1402003, 1402011, 1402012, 1402004, 1402005, 1402035, 1402036];
var bow = [1452002, 1452001, 1452005, 1452007, 1452008, 1452004, 1452031, 1452012, 1452017, 1452019, 1452044];
var claw = [1472000, 1472004, 1472009, 1472014, 1472020, 1472022, 1472028, 1472031, 1472033, 1472053, 1472052];
var dagger = [1332007, 1332013, 1332012, 1332031, 1332003, 1332015, 1332018, 1332023, 1332027, 1332052, 1332050];
var polearm = [1442000, 1442007, 1442001, 1442009, 1442005, 1442010, 1442008, 1442019, 1442020, 1442044, 1442045, 1442002];
var spear = [1432000, 1432008, 1432002, 1432005, 1432004, 1432006, 1432007, 1432010, 1432011, 1432030, 1432038];
var staff = [01382000, 01382004, 01382017, 01382019, 01382011, 01382068, 01382053, 01382056, 01382025, 01382037, 01382036];
var wand = [01372005, 01372002, 01372001, 01372012, 01372007, 01372031, 01372008, 01372035, 01372009, 01372010, 01372032];
var knuckle = [01482000, 01482002, 01482004, 01482006, 01482007, 01482008, 01482009, 01482010, 01482011, 01482012, 01482013];
var gun = [01492000, 01492002, 01492004, 1492006, 01492007, 01492008, 1492009, 01492010, 01492011, 01492012, 01492013];
var katara = [01342000, 01342001, 01342002, 01342003, 01342004, 01342005, 01342006, 01342007, 01342008, 01342010];
 

var status = -1;

function start() {
	action(1,0,0);
}

function action(m,t,s) {
	if (m != 1) {
		cm.dispose();
		return;
	} else {
		status++;
	}
	if (status == 0) {
		cm.sendSimple("#eHello #b#h ##k. This is the Tier Weapon system. In tropikms, weapons do not drop from enemies. The only way the acquire them is from earning them by talking to me. Highest level of weapon are visitor weapons. But you must get the visitor weapon from another npc. I am not powerful enough to hold them.#r\r\n\r\n#L0#I think i meet the requirement for a weapon!#l\r\n#L1#Tell me more about this system");
	} else if (status == 1) {
		if (s == 0) {
			cm.sendSimple("#eWhat job are you looking for?\r\n\r\n#b#L2#one-handed Sword#l\r\n#L3#two-handed sword#l\r\n#L4#Bow#l\r\n#L5#Claw#l\r\n#L6#Dagger#l\r\n#L6#Polearm#l\r\n#L7#Spear#l\r\n#L8#Staff#l\r\n#L9#Wand#l\r\n#L10#Knuckle#l\r\n#L11#Gun#l\r\n#L12#Katara#l\r\n");
		} else {
			cm.sendOk("#e#bFacts that weren't precised on the first window:#k\r\n\r\n1 - When upgrading your weapon, there a slight chance that the stats on it will be boosted. They are really worthy, so selling them to other players is a good option if you need quick coconuts.");
			cm.dispose();
		}
	} else if (status == 2) {
		if (s == 2) {
			var text = "#eWhich one would you like? Click on the one you'd like to see the requirement. I know it would be easier if they were on this page but it would look too messy. Plus i'm using array for this NPC. (Javascript!)#r\r\n\r\nRemember: You need the previous tier of weapon before the one you're trying to buy.#k\r\n\r\n";
			var selectiongenerator = 13;
			var tiernum = 1;
				for(i = 0; i < onehandedsword.length; i++) {
					text += "#L"+selectiongenerator+"##v"+onehandedsword[i]+"# - Tier "+tiernum+" weapon\r\n#l\r\n";
					tiernum++;
					selectiongenerator++;
				}
			cm.sendSimple(text);
		} else if (s == 3) {
			var text = "#eWhich one would you like? Click on the one you'd like to see the requirement. I know it would be easier if they were on this page but it would look too messy. Plus i'm using array for this NPC. (Javascript!)#r\r\n\r\nRemember: You need the previous tier of weapon before the one you're trying to buy.#k\r\n\r\n";
			var selectiongenerator = 23;
			var tiernum = 1;
				for(i = 0; i < twohandedsword.length; i++) {
					text += "#L"+selectiongenerator+"##v"+twohandedsword[i]+"# - Tier "+tiernum+" weapon\r\n#l\r\n";
					tiernum++;
					selectiongenerator++;
				}
			cm.sendSimple(text);
		} else if (s == 4) {
			var text = "#eWhich one would you like? Click on the one you'd like to see the requirement. I know it would be easier if they were on this page but it would look too messy. Plus i'm using array for this NPC. (Javascript!)#r\r\n\r\nRemember: You need the previous tier of weapon before the one you're trying to buy.#k\r\n\r\n";
			var selectiongenerator = 23;
			var tiernum = 1;
				for(i = 0; i < bow.length; i++) {
					text += "#L"+selectiongenerator+"##v"+bow[i]+"# - Tier "+tiernum+" weapon\r\n#l\r\n";
					tiernum++;
					selectiongenerator++;
				}
			cm.sendSimple(text);
		} else if (s == 5) {
			var text = "#eWhich one would you like? Click on the one you'd like to see the requirement. I know it would be easier if they were on this page but it would look too messy. Plus i'm using array for this NPC. (Javascript!)#r\r\n\r\nRemember: You need the previous tier of weapon before the one you're trying to buy.#k\r\n\r\n";
			var selectiongenerator = 23;
			var tiernum = 1;
				for(i = 0; i < claw.length; i++) {
					text += "#L"+selectiongenerator+"##v"+claw[i]+"# - Tier "+tiernum+" weapon\r\n#l\r\n";
					tiernum++;
					selectiongenerator++;
				}
			cm.sendSimple(text);
		} else if (s == 6) {
			var text = "#eWhich one would you like? Click on the one you'd like to see the requirement. I know it would be easier if they were on this page but it would look too messy. Plus i'm using array for this NPC. (Javascript!)#r\r\n\r\nRemember: You need the previous tier of weapon before the one you're trying to buy.#k\r\n\r\n";
			var selectiongenerator = 23;
			var tiernum = 1;
				for(i = 0; i < dagger.length; i++) {
					text += "#L"+selectiongenerator+"##v"+dagger[i]+"# - Tier "+tiernum+" weapon\r\n#l\r\n";
					tiernum++;
					selectiongenerator++;
				}
			cm.sendSimple(text);
		}
	} else if (status == 3) {
		cm.sendOk(""+s+"");
	}
}