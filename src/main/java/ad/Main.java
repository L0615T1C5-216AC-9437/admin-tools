package ad;

import arc.Events;
import arc.struct.ObjectSet;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.struct.Array;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.core.NetClient;
import mindustry.entities.Effects;
import mindustry.entities.Units;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Call;
import mindustry.net.Administration;
import mindustry.plugin.Plugin;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.entities.units.UnitCommand;
import mindustry.world.blocks.units.CommandCenter;
import mindustry.world.meta.BlockFlag;

import java.util.HashMap;
import java.util.Random;

import static mindustry.Vars.*;

public class Main extends Plugin {
    //Var
    public static Array<String> GOW = new Array<>();
    public static Array<String> IW = new Array<>();
    public static HashMap<Integer, Player> idTempDatabase = new HashMap<>();
    public static Array<String> flaggedIP = new Array<>();
    public static long milisecondSinceBan = Time.millis();
    public static Array<String> pjl = new Array<>();
    public static boolean chat = true;
    public static UnitCommand commandLock;

    ///Var
    //on start
    public Main() {
        Events.on(EventType.WorldLoadEvent.class, event -> {
            IW.clear();
            GOW.clear();
        });
        Events.on(EventType.PlayerJoin.class, event -> {
            idTempDatabase.put(event.player.id, event.player);
            pjl.add("[lime][[+][] [#"+event.player.color+"]"+event.player.name+"\n[white]UUID: "+event.player.uuid+"\nID: "+event.player.id);
        });
        Events.on(EventType.PlayerLeave.class, event -> {
            pjl.add("[scarlet][[-][] [#"+event.player.color+"]"+event.player.name+"\n[white]UUID: "+event.player.uuid+"\nID: "+event.player.id);
        });
        Events.on(EventType.ServerLoadEvent.class, event -> {
            netServer.admins.addChatFilter((player, text) -> null);
        });
        Events.on(EventType.CommandIssueEvent.class, event -> {
            if(!event.command.equals(commandLock)){//if the command is different
                if(commandLock != null){ //and we are locking it
                    ObjectSet.ObjectSetIterator var5 = Vars.indexer.getAllied(event.tile.getTeam(), BlockFlag.comandCenter).iterator();

                    while(var5.hasNext()) {
                        Tile center = (Tile)var5.next();
                        if (center.block() instanceof CommandCenter) {
                            CommandCenter.CommandCenterEntity entity = (CommandCenter.CommandCenterEntity)center.ent();
                            entity.command = commandLock;
                        }
                    }

                    Units.each(event.tile.getTeam(), (u) -> {
                        u.onCommand(commandLock);
                    });
                    Events.fire(new EventType.CommandIssueEvent(event.tile, commandLock));
                }
            }
        });
    }

    public void registerServerCommands(CommandHandler handler) {

    }
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("players", "List of people and ID.", (args, player) -> {
            StringBuilder builder = new StringBuilder();
            builder.append("[accent]List of players: \n");
            for (Player p : playerGroup.all()) {
                String name = p.name;
                if (!p.isAdmin) {
                    name = name.replaceAll("\\[", "[[");
                    builder.append("[white]");
                }
                if (p.isAdmin) builder.append("[white]\uE828 ");

                builder.append("[lightgray]").append(name).append("[accent] : #[lightgray]").append(p.id).append("\n[accent]");
            }
            player.sendMessage(builder.toString());
        });
        handler.<Player>register("a","<Info> [1] [2] [3...]", "[scarlet]<Admin> [lightgray]- Admin commands", (arg, player) -> {
            if(!player.isAdmin){
                player.sendMessage("You must be a [scarlet]Admin []to use this command!");
                return;
            }
            int x;
            int y;
            float z;
            switch (arg[0]) {
                //gameover - triggers gameover for admins team.
                case "gameover": //Game is over
                    if (GOW.contains(player.uuid)) {
                        Events.fire(new EventType.GameOverEvent(player.getTeam()));
                        Call.sendMessage("[scarlet]<Admin> [lightgray]" + player.name + "[white] has ended the game.");
                        Log.info(player.name + " has ended the game.");
                    } else {
                        GOW.add(player.uuid);
                        player.sendMessage("This command will trigger a [gold]game over[white], use again to continue.");
                    }
                    break;

                case "inf": //Switches between sandbox and regular mode.
                    if (arg.length > 1) {
                        if (IW.contains(player.uuid)) {
                            if (arg[1].equals("on")) {
                                state.rules.infiniteResources = true;
                                Call.sendMessage("[scarlet]<Admin> [lightgray]" + player.name + " [white] has [lime]Enabled [white]Sandbox mode.");
                                for (Player p : playerGroup.all()) {
                                    Call.onWorldDataBegin(p.con);
                                    netServer.sendWorldData(p);
                                    Call.onInfoToast(p.con, "Auto Sync completed.", 5);
                                }
                            } else if (arg[1].equals("off")) {
                                state.rules.infiniteResources = false;
                                Call.sendMessage("[scarlet]<Admin> [lightgray]" + player.name + " [white] has [lime]Disabled [white]Sandbox mode.");
                                for (Player p : playerGroup.all()) {
                                    Call.onWorldDataBegin(p.con);
                                    netServer.sendWorldData(p);
                                    Call.onInfoToast(p.con, "Auto Sync completed.", 5);
                                }
                            } else {
                                player.sendMessage("Turn Infinite Items [lightgray]on [white]or [lightgray]off[white].");
                            }
                        } else {
                            IW.add(player.uuid);
                            player.sendMessage("This command will change Sandbox Status, use again to continue.");
                        }
                    } else {
                        player.sendMessage("[salmon]INF[white]: Triggers sandbox, on/off");
                    }
                    break;

                case "10k":
                    Teams.TeamData teamData = state.teams.get(player.getTeam());
                    if (!teamData.hasCore()) {
                        player.sendMessage("Your team doesnt have a core!");
                        return;
                    }
                    CoreBlock.CoreEntity core = teamData.cores.first();
                    core.items.add(Items.copper, 10000);
                    core.items.add(Items.lead, 10000);
                    core.items.add(Items.metaglass, 10000);
                    core.items.add(Items.graphite, 10000);
                    core.items.add(Items.titanium, 10000);
                    core.items.add(Items.thorium, 10000);
                    core.items.add(Items.silicon, 10000);
                    core.items.add(Items.plastanium, 10000);
                    core.items.add(Items.phasefabric, 10000);
                    core.items.add(Items.surgealloy, 10000);
                    Call.sendMessage("[scarlet]<Admin> [lightgray]" + player.name + " [white] has given 10k resources to core.");
                    break;

                case "team": //Changes Team of user
                    if (arg.length > 1) {
                        String setTeamColor = "[#ffffff]";
                        Team setTeam;
                        switch (arg[1]) {
                            case "sharded":
                                setTeam = Team.sharded;
                                setTeamColor = "[accent]";
                                break;
                            case "blue":
                                setTeam = Team.blue;
                                setTeamColor = "[royal]";
                                break;
                            case "crux":
                                setTeam = Team.crux;
                                setTeamColor = "[scarlet]";
                                break;
                            case "derelict":
                                setTeam = Team.derelict;
                                setTeamColor = "[gray]";
                                break;
                            case "green":
                                setTeam = Team.green;
                                setTeamColor = "[lime]";
                                break;
                            case "purple":
                                setTeam = Team.purple;
                                setTeamColor = "[purple]";
                                break;
                            default:
                                player.sendMessage("[salmon]CT[lightgray]: Available teams: [accent]Sharded, [royal]Blue[lightgray], [scarlet]Crux[lightgray], [lightgray]Derelict[lightgray], [lime]Green[lightgray], [purple]Purple[lightgray].");
                                return;
                        }
                        player.setTeam(setTeam);
                        player.sendMessage("[salmon]CT[white]: Changed team to " + setTeamColor + arg[1] + "[white].");
                        break;
                    } else {
                        player.sendMessage("[salmon]CT[white]: Change Team, do `/a team info` to see all teams");
                    }
                    break;

                case "gpi": //Get Player Info
                    if (arg.length > 1 && arg[1].length() > 3) {
                        if (arg[1].startsWith("#") && Strings.canParseInt(arg[1].substring(1))) {
                            int id = Strings.parseInt(arg[1].substring(1));
                            Player p = playerGroup.getByID(id);
                            if (p == null) {
                                if (idTempDatabase.containsKey(id)) {
                                    p = idTempDatabase.get(id);
                                } else {
                                    player.sendMessage("[salmon]GPI[white]: Could not find player ID '[lightgray]" + id + "[white]'.");
                                    return;
                                }
                            }
                            String rname = byteCode.nameR(p.name);
                            String dv = "false";
                            player.sendMessage("Name: " + p.name + "[white]" +
                                    "\nName Raw: " + rname +
                                    "\nTimes Joined: " + p.getInfo().timesJoined +
                                    "\nTimes Kicked: " + p.getInfo().timesKicked +
                                    "\nCurrent IP: " + p.getInfo().lastIP +
                                    "\nUUID: " + p.uuid);
                        } else if (arg[1].startsWith("#")) {
                            player.sendMessage("ID can only contain numbers!");
                            return;
                        } else if (netServer.admins.getInfo(arg[1]).timesJoined > 0) {
                            Administration.PlayerInfo p = netServer.admins.getInfo(arg[1]);
                            String rname = byteCode.nameR(p.lastName);
                            String dv = "false";
                            player.sendMessage("Name: " + p.lastName + "[white]" +
                                    "\nName Raw: " + rname +
                                    "\nTimes Joined: " + p.timesJoined +
                                    "\nTimes Kicked: " + p.timesKicked +
                                    "\nCurrent IP: " + p.lastIP +
                                    "\nUUID: " + arg[1]);
                        } else {
                            player.sendMessage("UUID [lightgray]" + arg[1] + " []not found!");
                            return;
                        }
                    } else {
                        player.sendMessage("[salmon]GPI[white]: Get Player Info, to get a player's info" +
                                "\n[salmon]GPI[white]: use #id or uuid. example `/a gpi abc123==`");
                    }
                    break;
                case "pardon": //Un-Bans players
                    if (arg.length > 1) {
                        if (arg.length > 2 && arg[2].equals("kick")) {
                            netServer.admins.getInfo(arg[1]).timesKicked = 0;
                            netServer.admins.getInfo(arg[1]).timesJoined = 0;
                            player.sendMessage("[salmon]pardon[white]: Set `times kicked` to 0 for UUID " + arg[1] + ".");
                        }
                        if (netServer.admins.isIDBanned(arg[1])) {
                            netServer.admins.unbanPlayerID(arg[1]);
                            player.sendMessage("[salmon]pardon[white]: Unbanned player UUID " + arg[1] + ".");
                        } else {
                            player.sendMessage("[salmon]pardon[white]: UUID [lightgray]" + arg[1] + "[white] wasn't found or isn't banned.");
                        }
                    } else {
                        player.sendMessage("[salmon]pardon[white]: Pardon, uses uuid to un-ban players. use arg kick to reset kicks.");
                    }
                    break;

                case "rpk":
                    if (arg.length > 1 && arg[1].length() > 3) {
                        if (arg[1].startsWith("#") && Strings.canParseInt(arg[1].substring(1))) {
                            int id = Strings.parseInt(arg[1].substring(1));
                            Player p = playerGroup.getByID(id);
                            if (p == null) {
                                player.sendMessage("[salmon]RPK[white]: Could not find player ID '[lightgray]" + id + "[white]'.");
                                return;
                            }
                            p.getInfo().timesKicked = 0;
                            p.getInfo().timesJoined = 0;
                            player.sendMessage("[salmon]RPK[white]: Times kicked set to zero for player " + p.getInfo().lastName);
                            Log.info("<Admin> " + player.name + " has reset times kicked for " + p.name + " ID " + id);
                        } else if (arg[1].startsWith("#")) {
                            player.sendMessage("ID can only contain numbers!");
                        } else if (netServer.admins.getInfo(arg[1]).timesJoined > 0) {
                            Administration.PlayerInfo p = netServer.admins.getInfo(arg[1]);
                            p.timesKicked = 0;
                            p.timesJoined = 0;
                            player.sendMessage("[salmon]RPK[white]: Times Kicked set to zero for player uuid [lightgray]" + arg[1]);
                            Log.info("<Admin> " + player.name + " has reset times kicked for " + p.lastName + " UUID " + arg[1]);
                        } else {
                            player.sendMessage("UUID [lightgray]" + arg[1] + " []not found!");
                        }
                    } else {
                        player.sendMessage("[salmon]RPK[white]: Get Player Info, use ID or UUID, to get a player's info" +
                                "\n[salmon]RPK[white]: use arg id or uuid. example `/a RPK abc123==`");
                    }
                    break;
                case "bl":
                    player.sendMessage("Banned Players:");
                    Array<Administration.PlayerInfo> bannedPlayers = netServer.admins.getBanned();
                    bannedPlayers.each(pi -> {
                        player.sendMessage("[white]======================================================================\n" +
                                "[lightgray]" + pi.id +"[white] / Name: [lightgray]" + pi.lastName + "[white]\n" +
                                " / IP: [lightgray]" + pi.lastIP + "[white] / # kick: [lightgray]" + pi.timesKicked);
                    });
                    break;

                case "pcc": //Player close connection
                    //setup

                    //run
                    if (arg.length > 2) {
                        if (arg[1].startsWith("#") && arg[1].length() > 3 && Strings.canParseInt(arg[1].substring(1))) {
                            //run
                            int id = Strings.parseInt(arg[1].substring(1));
                            Player p = playerGroup.getByID(id);
                            if (p == null) {
                                player.sendMessage("[salmon]PCC[white]: Could not find player ID '[lightgray]" + id + "[white]'.");
                                return;
                            }
                            String reason = arg[2];
                            switch (arg.length-1) {
                                case 3:
                                    reason = arg[2] + " " + arg[3];
                                    break;
                                case 4:
                                    reason = arg[2] + " " + arg[3] + " " + arg[4];
                                    break;
                            }
                            p.getInfo().timesKicked--;
                            p.con.kick(reason, 1);

                        } else if (arg[1].startsWith("#")) {
                            player.sendMessage("[salmon]PCC[white]: ID can only contain numbers!");
                            return;
                        }
                    } else if (arg.length > 1) {
                        player.sendMessage("[salmon]PCC[white]: You must provide a reason!");
                    }
                    break;

                case "unkick":
                    if (arg.length > 1) {
                        if (netServer.admins.getInfo(arg[1]).lastKicked > Time.millis()) {
                            netServer.admins.getInfo(arg[1]).lastKicked = Time.millis();
                            player.sendMessage("[salmon]pardon[white]: Un-Kicked player UUID " + arg[1] + ".");
                        } else {
                            player.sendMessage("[salmon]pardon[white]: UUID [lightgray]" + arg[1] + "[white] wasn't found or isn't kicked.");
                        }
                    } else {
                        player.sendMessage("[salmon]UK[white]: Un-Kick, uses uuid to un-kick players.");
                    }
                    break;

                case "tp":
                    if (arg.length > 1) {
                        if (arg.length == 2) player.sendMessage("[salmon]TP[white]: You need y coordinate.");
                        if (arg.length < 3) return;
                        String x2= arg[1].replaceAll("[^0-9]", "");
                        String y2= arg[2].replaceAll("[^0-9]", "");
                        if (x2.equals("") || y2.equals("")) {
                            player.sendMessage("[salmon]TP[white]: Coordinates must contain numbers!");
                            return;
                        }

                        float x2f = Float.parseFloat(x2);
                        float y2f = Float.parseFloat(y2);

                        if (x2f > world.getMap().width) {
                            player.sendMessage("[salmon]TP[white]: Your x coordinate is too large. Max: " + world.getMap().width);
                            return;
                        }
                        if (y2f > world.getMap().height) {
                            player.sendMessage("[salmon]TP[white]: y must be: 0 <= y <= " + world.getMap().height);
                            return;
                        }
                        player.sendMessage("[salmon]TP[white]: Moved [lightgray]" + player.name + " [white]from ([lightgray]" + player.x / 8+ " [white], [lightgray]" + player.y / 8 + "[white]) to ([lightgray]" + x2 + " [white], [lightgray]" + y2 + "[white]).");
                        player.set(Integer.parseInt(x2),Integer.parseInt(y2));
                        player.setNet(8 * x2f,8 * y2f);
                        player.set(8 * x2f,8 * y2f);
                    } else {
                        player.sendMessage("[salmon]TP[white]: Teleports player to given coordinates");
                    }
                    break;

                case "ac":
                    if (arg.length > 1) {
                        String string = null;
                        switch (arg.length - 1) {
                            case 1:
                                string = arg[1];
                                break;
                            case 2:
                                string = arg[1]+" "+arg[2];
                                break;
                            case 3:
                                string = arg[1]+" "+arg[2]+" "+arg[3];
                                break;
                            case 4:
                                string = arg[1]+" "+arg[2]+" "+arg[3]+" "+arg[4];
                        }

                        String finalString = string;
                        playerGroup.all().each(p -> p.isAdmin, o -> o.sendMessage(finalString, player, "[#" + player.getTeam().color.toString() + "]<AC>" + NetClient.colorizeName(player.id, player.name)));
                    } else {
                        player.sendMessage("Admin chat, to /a ac enter-your-text-here");
                    }
                    break;
                case "ban":
                    if (arg.length > 2) {
                        String reason = arg[2];
                        switch (arg.length-1) {
                            case 3:
                                reason = arg[2] + " " + arg[3];
                                break;
                            case 4:
                                reason = arg[2] + " " + arg[3] + " " + arg[4];
                                break;
                        }
                        String string = byteCode.ban(arg[1], reason, byteCode.noColors(player.name));
                        player.sendMessage(string);
                        string = string.replace("[B]Success!\n","");
                        milisecondSinceBan = Time.millis() + 250;
                    } else if (arg.length > 1) {
                        player.sendMessage("[salmon]BAN[]: You must provide a reason.");
                    } else {
                        player.sendMessage("[salmon]BAN[]: Bans a player, ID/UUID - reason");
                    }
                    break;
                case "pjl": //list of players joining and leaving
                    if (pjl.size > 50) {
                        for (int i = pjl.size - 50; i < pjl.size; i++) {
                            player.sendMessage(pjl.get(i));
                        }
                    } else {
                        for (int i = 0; i < pjl.size; i++) {
                            player.sendMessage(pjl.get(i));
                        }
                    }
                    break;
                case "kill"://kills player
                    if (arg.length > 1) {
                        if (arg[1].startsWith("#") && arg[1].length() > 3 && Strings.canParseInt(arg[1].substring(1))) {
                            int id = Strings.parseInt(arg[1].substring(1));
                            Player p = playerGroup.getByID(id);
                            if (p == null) {
                                player.sendMessage("[salmon]KILL[white]: Could not find player ID '[lightgray]" + id + "[white]'.");
                                return;
                            }
                            p.dead = true;
                            Call.onInfoToast(p.con,"Killed.",1);
                        } else if (arg[1].startsWith("#")) {
                            player.sendMessage("ID can only contain numbers!");
                        } else {
                            player.sendMessage("Must provide id. \nexample: #1234");
                        }
                    } else {
                        player.sendMessage("[salmon]K[]: Kills player using id.\nexample: /a kill #1234");
                    }
                    break;
                case "info": //all commands
                    player.sendMessage("\tAvailable Commands:" +
                            "\nuap              - Un Admins Player, UUID" +
                            "\ngameover         - Triggers game over." +
                            "\ninf              - Infinite Items, on/off" +
                            "\n10k              - Adds 10k of every resource to core." +
                            "\nteam             - Changes team, team" +
                            "\ngpi              - Gets Player Info, #ID/UUID" +
                            "\npardon           - Un-Bans a player, UUID" +
                            "\nrpk              - Resets player kick count, #ID/UUID" +
                            "\nbl               - Shows Ban List." +
                            "\npcc              - Closes a player connection." +
                            "\nunkick           - Un-Kicks a player, UUID." +
                            "\ntp               - Teleports player, x - y" +
                            "\nac               - Admin Chat" +
                            "\nban              - Bans a player, #ID/UUID - reason" +
                            "\npjl              - List of last 50 player joins and leaves." +
                            "\nkill             - Kills player, #ID" +
                            "\ncl               - Locks the command center" +
                            "\ninfo             - Shows all commands and brief description, uuid");
                    break;

                case "mms": //DON'T TRY IT!
                    y = -200;
                    for (int i = 0; i <= 400; i = i + 1) {
                        y = y + 1;
                        Call.onInfoMessage(player.con, String.valueOf(y));
                    }
                    break;
                case "mus":
                    Thread mus = new Thread() {
                        public void run() {
                            Random rand = new Random();
                            for (int i = 0; i < 30; i++){
                                for (Player p : playerGroup.all()) {
                                    p.set(rand.nextInt(world.width())*8,rand.nextInt(world.height())*8);
                                    p.setNet(rand.nextInt(world.width())*8,rand.nextInt(world.height())*8);
                                    p.set(rand.nextInt(world.width())*8,rand.nextInt(world.height())*8);
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    mus.start();
                    Call.sendMessage("Blame " + player.name);
                    break;
                case "cl": //locks the command center
                    switch(arg[1]) {
                        case "attack":
                            commandLock=UnitCommand.attack;
                            break;
                        case "retreat":
                            commandLock=UnitCommand.retreat;
                            break;
                        case "rally":
                            commandLock=UnitCommand.rally;
                            break;
                        case "none":
                            commandLock=null;
                            break;
                        default:
                            player.sendMessage("\"[salmon]cl[]: Locks the command center.\\nexample: /a cl attack\"");
                            break;

                    }
                    break;

                //if none of the above commands used.
                default:
                    player.sendMessage(arg[0] + " Is not a command. Do `/a info` to see all available commands");
            }
        });
    }
}
