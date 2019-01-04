/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import database.DatabaseConnection;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Oxysoft
 * @purpose Faction System
 */

public class MapleFaction { //load instances in channel server init
    
    private int factionId, factionPoints;
    private String name;
    private MapleFaction instance;
    private static Map<Integer, String> memberObject = new HashMap<>();
    
    public MapleFaction(final int id) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM MapleFaction WHERE factionid = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery(); 
            if (!rs.first()) {
                rs.close();
                ps.close();
                return;
            }
            factionId = id;
            factionPoints = rs.getInt("points");
            name = rs.getString("name");
            rs.close();
            ps.close();
        }catch(SQLException e) {
            System.out.println("Couldn't get MapleFaction information from database : ");
            e.printStackTrace();
        }
    }
    
    public static void loadFactions() {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT factionid FROM MapleFaction");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
               MapleFaction factionObject = new MapleFaction(rs.getInt("factionid"));
               System.out.println("Registered faction by id : "  + factionObject.factionId);
               World.Faction.populateFactions(factionObject);
            }
        } catch (SQLException e) {
            System.out.println("Catched SQL exception when loading factions : " + e);
        }
    }
    
    public static void loadFactionMembers(MapleFaction fac) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, name, factionid FROM characters");
            
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                if (rs.getInt(("factionid")) == fac.getId()) {
                    memberObject.put(rs.getInt("id"), rs.getString("name"));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
//    public void loadMembers(MapleCharacter chr) {
//        Connection con = DatabaseConnection.getConnection();
//        try {
//            PreparedStatement ps = con.prepareStatement("SELECT factionid FROM characters");
//            ResultSet rs = ps.executeQuery();
//            while(rs.next()) {
//                if(rs.getInt("factionid") == factionId) {
//                    members.add(chr);
//                }
//            }
//            rs.close();
//            ps.close();
//        } catch (SQLException e) {
//            System.out.println("Catched SQL exception when loading factions members : " + e);
//        }
//    }
    
//    public void saveMembers() {
//        Connection con = DatabaseConnection.getConnection();
//        try {
//            PreparedStatement ps = con.prepareStatement("UPDATE ");
//            ps.executeUpdate();
//            ps.close();
//        } catch (SQLException e) {
//            System.out.println("Catched SQL exception when loading factions members : " + e);
//        }
//    }
    
    public final void saveFaction() {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE MapleFaction SET points = ? WHERE factionid = ?");
            ps.setInt(1, factionPoints);
            ps.setInt(2, factionId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public MapleFaction getInstance() {
        return instance;
    }
    
//    public final List<MapleCharacter> getMembers() {
//        return members;
//    }
//    
//    public void addMember(MapleCharacter t) {
//            members.add(t);
//            t.setFactionId(factionId);
//    }
//    
//    public void removeMember(String name) {
//            for (MapleCharacter c : members) {
//                if (c.getName().equals(name)) {
//                    members.remove(c);
//                }
//            }
//    }
//    
//    public String getMembersName() {
//        StringBuilder sb = new StringBuilder();
//        int iteratornum = 1;
//        for (MapleCharacter c : members) {
//            sb.append(iteratornum + c.getName());
//            iteratornum++;
//        }
//        return sb.toString();
//    }
    
    public void gainPoint(int newval) {
        factionPoints += newval;
    }
    
    public int getFactionPoints() {
        return factionPoints;
    }
    
    public int getId() {
        return factionId;
    }
    
    public String getName() {
        return name;
    }
}
