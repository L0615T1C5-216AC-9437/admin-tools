package ad;

import arc.util.Strings;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.entities.type.Player;
import mindustry.net.Administration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import static mindustry.Vars.*;

public class byteCode {
    //code
    public static String nameR(String name) {return name.replaceAll("\\[", "[[");}
    public static String ban(String IDuuid, String reason, String who) {
        //setup
        Date thisDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("[MM/dd/Y | HH:mm:ss] ");
        String uuid = null;
        String nameR = null;
        boolean proceed = false;

        //ban
        if (IDuuid.startsWith("#") && IDuuid.length() > 3 && Strings.canParseInt(IDuuid.substring(1))) {
            int id = Strings.parseInt(IDuuid.substring(1));
            Player p = playerGroup.getByID(id);
            if (p == null) {
                return "Player ID " + id + " not found.";
            } else if (p.isAdmin) {
                return "[scarlet]Did you really expect to be able to ban a admin?";
            }
            Main.flaggedIP.add(p.getInfo().lastIP);
            nameR = nameR(p.name);
            uuid = p.uuid;
            proceed = true;
            p.con.kick(reason);
        } else if (IDuuid.startsWith("#")) {
            return "ID can only contain numbers!"; //if contains letters
        } else if (netServer.admins.getInfo(IDuuid).timesJoined > 0) {
            Administration.PlayerInfo p = netServer.admins.getInfo(IDuuid);
            nameR = nameR(p.lastName);
            uuid = IDuuid;
            //check for admin
            for (Administration.PlayerInfo pi : netServer.admins.getAdmins()) {
                if (pi.id.equals(IDuuid)) {
                    return "[scarlet]Did you really expected to ban a admin?";
                }
            }
            proceed = true;
        } else {
            return "UUID not found!"; // not found
        }
        if (proceed) {
            netServer.admins.banPlayer(uuid);
            try {
                File file = new File("bl.cn");
                FileWriter out = new FileWriter(file, true);
                PrintWriter pw = new PrintWriter(out);
                pw.println(dateFormat.format(thisDate) + nameR + " | " + uuid + " | " + reason + " | by: " + who + " ;");
                out.close();
            } catch (IOException i) {
                i.printStackTrace();
            }
            Main.milisecondSinceBan = Time.millis() + 250;
            return "[B]Success!\n" + dateFormat.format(thisDate) + nameR + " | " + reason + " | by: " + who + " ;";
        }
        Log.err("Ban got past return!");
        return "error";
    }
    public static String noColors(String string){
        return Strings.stripColors(string);}
}
/*
if (arg[1].startsWith("#") && arg[1].length() > 3 && Strings.canParseInt(arg[1].substring(1))){
    //run
    int id = Strings.parseInt(arg[1].substring(1));
} else if (arg[1].startsWith("#")){
    player.sendMessage("ID can only contain numbers!");
} else if (netServer.admins.getInfo(arg[1]).timesJoined > 0) {
    //run
} else {
    player.sendMessage("UUID not found!");
}
*/
