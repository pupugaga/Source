package com.l2jfrozen.gameserver.model.actor.instance;

import com.l2jfrozen.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jfrozen.gameserver.templates.L2NpcTemplate;
import javolution.text.TextBuilder;
import java.util.StringTokenizer;

/**
 * Created by Server1 on 7/29/2017.
 */
public class L2PartyTeleportInstance extends L2NpcInstance {

    public L2PartyTeleportInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }


    public void onBypassFeedback(L2PcInstance player, String command) {
        int zone = 0;
        if (command.startsWith("teleport")) {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();
            zone = Integer.parseInt(st.nextToken());
            NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
            switch (zone)
            {
                case 1:
                {
                    if ((player.isInParty() && player.getParty().getMemberCount() >= 4 && player == player.getParty().getLeader()) || player.isGM())
                    {
                        if(player.isGM()){
                            player.teleToLocation(12537, -248480, -9576);
                        }
                        else {
                            int playersInWaitingRoom = 0;
                            for (L2PcInstance p : player.getParty().getPartyMembers()) {
                                if (p.isInsideZone(ZONE_PARTYWAITINGROOM))
                                    playersInWaitingRoom++;
                            }
                            if (playersInWaitingRoom >= 4) {
                                for (L2PcInstance p : player.getParty().getPartyMembers()) {
                                    if (p.isInsideZone(ZONE_PARTYWAITINGROOM)) {
                                        p.teleToLocation(12537, -248480, -9576);
                                    }
                                }
                            } else {
                                msg.setHtml(notEnoughMembersInWaitingRoom(player));
                                msg.replace("%objectId%", String.valueOf(getObjectId()));
                                player.sendPacket(msg);
                            }

                        }
                    }
                    else if(!player.isInParty() || player.getParty().getMemberCount() < 4)
                    {
                        msg.setHtml(notEnoughMembersInParty(player));
                        msg.replace("%objectId%", String.valueOf(getObjectId()));
                        player.sendPacket(msg);
                    }else{
                        msg.setHtml(notPartyLeader(player));
                        msg.replace("%objectId%", String.valueOf(getObjectId()));
                        player.sendPacket(msg);
                    }
                    break;
                }
            }
        }
    }

    public void showChatWindow(L2PcInstance player, int val) {
        NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
        msg.setHtml(teleportWindow(player));
        msg.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(msg);
    }


    private String notEnoughMembersInParty(L2PcInstance player) {
        TextBuilder tb = new TextBuilder();
        tb.append("<html><title>Party Zone</title><body>");
        tb.append("<center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=295 height=32><br></center>");
        tb.append("You have to be in a party of minimum 4 players in order to enter.");
        tb.append("<br><center><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=295 height=32><br></center>");
        tb.append("</body></html>");
        return tb.toString();
    }

    private String notPartyLeader(L2PcInstance player) {
        TextBuilder tb = new TextBuilder();
        tb.append("<html><title>Party Zone</title><body>");
        tb.append("<center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=295 height=32><br></center>");
        tb.append("Only the party leader can allow entrance.");
        tb.append("<br><center><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=295 height=32><br></center>");
        tb.append("</body></html>");
        return tb.toString();
    }

    private String notEnoughMembersInWaitingRoom(L2PcInstance player) {
        TextBuilder tb = new TextBuilder();
        tb.append("<html><title>Party Zone</title><body>");
        tb.append("<center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=295 height=32><br></center>");
        tb.append("There must be at least 4 party members in the waiting room.");
        tb.append("<br><center><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=295 height=32><br></center>");
        tb.append("</body></html>");
        return tb.toString();
    }

    private String teleportWindow(L2PcInstance player) {
        TextBuilder tb = new TextBuilder();
        tb.append("<html><title>Party Zone</title><body>");
        tb.append("<center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=295 height=32><br><br>");
        tb.append("<button value=\"Take My Party Inside\" action=\"bypass -h npc_%objectId%_teleport 1\" width=204 height=20 back=\"sek.cbui75\" fore=\"sek.cbui75\">");
        tb.append("</center>");
        tb.append("<br><center><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=295 height=32><br></center>");
        tb.append("</body></html>");
        return tb.toString();
    }

}
