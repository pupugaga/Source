package com.l2jfrozen.gameserver.model.actor.instance;

import com.l2jfrozen.gameserver.model.L2Party;
import com.l2jfrozen.gameserver.model.L2World;
import com.l2jfrozen.gameserver.model.entity.Announcements;
import com.l2jfrozen.gameserver.model.scripts.ArenaFight;
import com.l2jfrozen.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jfrozen.gameserver.templates.L2NpcTemplate;
import com.l2jfrozen.gameserver.util.Broadcast;
import javolution.text.TextBuilder;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Created by Server1 on 8/3/2017.
 */
public class L2ArenaManagerInstance extends L2NpcInstance {

    public static Vector<ArenaFight> fights = new Vector<ArenaFight>();
    public static Vector<L2PcInstance> participants = new Vector<L2PcInstance>();
    public static Vector<L2PcInstance> participantsLobby = new Vector<L2PcInstance>();
    public static Vector<L2PcInstance> inFightOrWaiting = new Vector<L2PcInstance>();
    public static HashMap<Integer, Boolean> freeArenas = new HashMap<Integer, Boolean>();
    public static HashMap <HashMap<String, Integer>, HashMap<L2PcInstance, Integer>> outerBetMap = new HashMap<>();
    public static  HashMap<L2PcInstance, Integer> secondInnerBetMap = new HashMap<>();
    public static  HashMap<String, Integer> innerBetMap = new HashMap<>();


    private int idFight = 0;

    public L2ArenaManagerInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
        freeArenas.put(1, true);
        freeArenas.put(2, true);
        freeArenas.put(3, true);
    }




    public void onBypassFeedback(L2PcInstance player, String command) {
        int zone = 0;
        if (command.startsWith("arena")) {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();

            String cmd = st.nextToken();
            switch (cmd)
            {
                case "register":
                {
                    register(player);
                }
                break;
                case "info":
                {
                    int id = Integer.parseInt(st.nextToken());
                    Announcements.getInstance().announceToAll("Id : " + id);

                    NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
                    msg.setHtml(fightInfos(id));
                    msg.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(msg);
                }
                break;
                case "main":
                {
                    showChatWindow(player, 0);
                }
                break;
                case "bet":
                {
                    int id = Integer.parseInt(st.nextToken());
                    int amount = Integer.parseInt(st.nextToken());
                    String name = st.nextToken();

                    bet(player,amount ,id, name);
                    NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
                    msg.setHtml(betWindow(id, name));
                    msg.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(msg);

                }
                break;
                case "profile":
                {
                    NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
                    msg.setHtml(myInfosWindow(player));
                    msg.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(msg);
                }
                break;
                case "lobby":
                {
                    cmd = st.nextToken();
                    switch (cmd){
                        case "list":
                        {
                            NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
                            msg.setHtml(lobbyWindow(player));
                            msg.replace("%objectId%", String.valueOf(getObjectId()));
                            player.sendPacket(msg);
                        }
                        break;
                        case "join":
                        {
                            if (participantsLobby.contains(player)){
                                player.sendMessage("You're already in the list !");
                                return;
                            }

                            if (inFightOrWaiting.contains(player))
                            {
                                player.sendMessage("You will start a duel soon.");
                                return;
                            }
                            participantsLobby.add(player);
                            NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
                            msg.setHtml(lobbyWindow(player));
                            msg.replace("%objectId%", String.valueOf(getObjectId()));
                            player.sendPacket(msg);
                        }
                        break;
                        case "leave":
                        {
                            if (participantsLobby.contains(player)){
                                participantsLobby.remove(player);
                                player.sendMessage("Removed from the list !");
                                NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
                                msg.setHtml(lobbyWindow(player));
                                msg.replace("%objectId%", String.valueOf(getObjectId()));
                                player.sendPacket(msg);
                            }
                        }
                        break;
                        case "fight":
                        {
                            if (participants.contains(player))
                            {
                                participants.remove(player);
                            }
                            if (participantsLobby.contains(player))
                            {
                                participantsLobby.remove(player);
                            }
                            if (inFightOrWaiting.contains(player))
                            {
                                player.sendMessage("You will start a duel soon.");
                                return;
                            }
                            String playerName = st.nextToken();
                            L2PcInstance chosen = L2World.getInstance().getPlayer(playerName);
                            createArena(player, chosen, 1);
                        }
                        break;
                    }
                }
                break;
            }
        }
    }


    public void createArena(L2PcInstance playerOne, L2PcInstance playerTwo, int idArena)
    {
        ArenaFight arena = new ArenaFight( playerOne, playerTwo, idArena, idFight);
        arena.beginArena();
        inFightOrWaiting.add(playerOne);
        inFightOrWaiting.add(playerTwo);
        if (participantsLobby.contains(playerOne))
            participantsLobby.remove(playerOne);
        if (participantsLobby.contains(playerTwo))
            participantsLobby.remove(playerTwo);
        fights.add(arena);
        idFight++;
    }

    public void register(L2PcInstance player){

        if (player == null){
            return ;
        }

        if (inFightOrWaiting.contains(player))
        {
            player.sendMessage("You will start a duel soon.");
            return;
        }

        if (participants.contains(player)){

            player.sendMessage("You're already in the queue.");
            return;
        }

        if (participantsLobby.contains(player))
        {
            player.sendMessage("You're already in a lobby");
            return;
        }

        participants.add(player);
        if (participants.size() >= 2)
        {
            for(int i=1; i<=freeArenas.size();i++) {

                if(freeArenas.get(i)){
                    createArena(participants.elementAt(0), participants.elementAt(1), i);
                    participants.remove(participants.firstElement());
                    participants.remove(participants.lastElement());
                    break;
                }
            }

            return;

        }
        else if (participants.size() == 1)
        {
            player.sendMessage("You have been added to the queue.");
            return;
        }
        else
        {

        }

    }


    public void showChatWindow(L2PcInstance player, int val) {
        NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
        msg.setHtml(arenaWindow(player));
        msg.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(msg);
    }

    public void bet(L2PcInstance player,int amount ,int fightId, String playerNameBet){

        /*innerBetMap.put(playerNameBet,fightId);
        secondInnerBetMap.put(player,amount);


        outerBetMap.put(innerBetMap,secondInnerBetMap);*/
        ArenaFight arena = null;

        for (ArenaFight a : fights)
        {
            if (a.getIdFight() == fightId)
                arena = a;
        }

        if (arena == null)
        {
            return;
        }

        arena.addBet(player, amount, L2World.getInstance().getPlayer(playerNameBet).getObjectId());

        Announcements.getInstance().announceToAll(player.getName()+" betted "+amount+" on "+playerNameBet);

    }



    public void onDie(L2PcInstance killer, L2PcInstance killed)
    {

    }

    private void shareBets(L2PcInstance winner, int fightId){

    }












    private String fightInfos(int id)
    {
        TextBuilder tb = new TextBuilder();
        ArenaFight arena = null;

        for (ArenaFight a : fights)
        {
            if (a.getIdFight() == id)
                arena = a;
        }

        if (arena == null)
        {
            return "";
        }

        L2PcInstance playerOne = arena.getFighters().firstElement();
        L2PcInstance playerTwo = arena.getFighters().lastElement();

        tb.append("<html><title>Duel id : " + id + "</title><body>");
        tb.append("<center> Player 1 : " + playerOne.getName() + "<br>");
        tb.append("Player 2: " + playerTwo.getName() + "<br>");
        tb.append("<combobox var=\"name\" list=\" " + playerOne.getName() +  ";" + playerTwo.getName() + "\" width=\"170\" height=\"15\"/><br>");
        tb.append("<edit var=\"qbox\" width=120 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");
        tb.append("<button value=\"Bet !\" action=\"bypass -h npc_%objectId%_arena bet " + id + " $qbox $name\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");
        tb.append("<button value=\"Back to list\" action=\"bypass -h npc_%objectId%_arena main\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");

        tb.append("</body></html>");

        return tb.toString();
    }


    private String lobbyWindow(L2PcInstance player){

        TextBuilder tb = new TextBuilder();

        tb.append("<html><title>Lobby</title><body><center>");

        if (participantsLobby.contains(player))
        {
            tb.append("<button value=\"Leave\" action=\"bypass -h npc_%objectId%_arena lobby leave\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");

        }
        else if (!participants.contains(player))
        {
            tb.append("<button value=\"Join\" action=\"bypass -h npc_%objectId%_arena lobby join\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");
        }

        tb.append("<button value=\"Back to list\" action=\"bypass -h npc_%objectId%_arena main\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");
        tb.append("");


        tb.append("<center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=295 height=32><br><br>");
        for (L2PcInstance p : participantsLobby){
            tb.append("<center>" + p.getName() + " <button value=\"Fight\" action=\"bypass -h npc_%objectId%_arena lobby fight " + p.getName() + "\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"></center><br>");
        }
        tb.append("<br><center><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=295 height=32><br></center>");


        tb.append("</body></html>");

        return tb.toString();

    }

    private String betWindow(int id, String name)
    {
        TextBuilder tb =
                new TextBuilder();
        ArenaFight arena = null;
        for (ArenaFight a : fights)
        {
            if (a.getIdFight() == id)
                arena = a;
        }

        if (arena == null)
        {
            return "";
        }

        L2PcInstance playerOne = arena.getFighters().firstElement();
        L2PcInstance playerTwo = arena.getFighters().lastElement();

        tb.append("<html><title>Duel id : " + id + "</title><body>");
        tb.append("<center> You bet on : " + name + " <br>");
        tb.append("<button value=\"Back to list\" action=\"bypass -h npc_%objectId%_arena main\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");

        tb.append("</body></html>");

        return tb.toString();
    }

    private String myInfosWindow(L2PcInstance player)
    {
        TextBuilder tb =
                new TextBuilder();

        tb.append("<html><title>" + player.getName() + "</title><body>");
        tb.append("<center>wins : " + player.getArenaInfos("arena_wins") + " <br>" +
                "looses : " + player.getArenaInfos("arena_loses") + "<br>");
        tb.append("</body></html>");

        return tb.toString();
    }


    private String arenaWindow(L2PcInstance player) {
        TextBuilder tb = new TextBuilder();
        tb.append("<html><title>Arena Manager</title><body><center>");
        tb.append("<button value=\"Register\" action=\"bypass -h npc_%objectId%_arena register\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");
        tb.append("<button value=\"Lobby\" action=\"bypass -h npc_%objectId%_arena lobby list\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");
        tb.append("<button value=\"My Profile\" action=\"bypass -h npc_%objectId%_arena profile\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");
        tb.append("Current Fights : " + fights.size() + "<br>");
        if (participants.size() != 0)
            tb.append("Current player waiting : " + participants.lastElement().getName() + "<br>");
        tb.append("</center>");
        tb.append("<center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=295 height=32><br><br>");

        for (ArenaFight arena : fights)
        {
            if (arena.getFighters().firstElement() != null && arena.getFighters().lastElement() != null)
            tb.append("<button value=\"" + arena.getFighters().firstElement().getName() + " VS " + arena.getFighters().lastElement().getName() + "\" action=\"bypass -h npc_%objectId%_arena info " + arena.getIdFight() + "\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\"><br>");

        }

        tb.append("<br><center><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=295 height=32><br></center>");
        tb.append("</body></html>");
        return tb.toString();
    }


    public static void refreshArenaAvailability(int idArena, boolean free) {
        freeArenas.put(idArena,free);
    }

    public static L2ArenaManagerInstance getInstance()
    {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder
    {
        protected static final L2ArenaManagerInstance _instance = new L2ArenaManagerInstance(-1, null);
    }


}
