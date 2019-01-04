var mapid = [682000200, 980042000, 280020000, 105040316];
var spawnpoint = [1, 0, 0, 0];

function start() {
	cm.sendSimple("#eHello #h #. I am the jump quest NPC. You can select one of the jump quest below and i will warp you to them. But beware, there is a requirement.\r\n#rYou must have 10 reborns#k\r\nat the end of the jump quest, you will see another NPC that'll warp you out and give you a little prize. \r\n\r\nYour prize is based on your jumping level. do @jumplevel or @myshit to know your level. You gain jump exp when gaining your reward at the end of the JQ.The harder is the JQ, the better will the rewards be.\r\n\r\n#rYour jumping Level:#n #d"+cm.getPlayer().getJumpLevel()+" #k|#d "+cm.getJumpLevelString()+"#r\r\n#eYour Jumping XP:#n #d"+cm.getPlayer().getJumpXP()+"#e\r\n#k\r\n\r\n#L0#Ghost Chimney #r[HARD]#k#l\r\n#L1#Pink Bean Jump Quest #g[EASY]#k#l\r\n#L2#Zakum Altair Jump Quest #b[MEDIUM]#k\r\n#L3#Deep Forest Of Patience 7 #r[VERY HARD]#k\r\n");
}

function action(m,t,s) {
	if (m < 1) {
		cm.dispose();
	}
	//if (cm.getPlayer().getReborns() >= 10) {
		cm.warp(mapid[s], spawnpoint[s]);
	//} else {
	//	cm.sendOk("#eYou do not have #r10#k reborns");
	//}
	cm.dispose();
}