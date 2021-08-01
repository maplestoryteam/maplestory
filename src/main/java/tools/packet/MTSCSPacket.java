/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools.packet;

import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillEntry;
import client.inventory.IItem;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.MaplePacket;
import handling.SendPacketOpcode;
import server.CashItemFactory;
import server.CashItemInfo;
import server.CashItemInfo.CashModInfo;
import server.CashShop;
import server.MTSStorage.MTSItemInfo;
import tools.HexTool;
import tools.KoreanDateUtil;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class MTSCSPacket {

    //    private static final byte[] warpCS = HexTool.getByteArrayFromHexString("");
    //private static final byte[] warpCS = HexTool.getByteArrayFromHexString("00 63 2D 31 01 08 02 04 01 2C 9E 98 00 08 00 00 62 2D 31 01 08 02 04 01 2F 9E 98 00 08 00 03 61 2D 31 01 08 02 04 01 2E 9E 98 00 08 00 04 60 2D 31 01 08 02 04 01 29 9E 98 00 08 00 05 67 2D 31 01 08 02 04 01 28 9E 98 00 08 00 00 66 2D 31 01 08 02 04 01 2B 9E 98 00 08 00 02 65 2D 31 01 08 02 04 01 2A 9E 98 00 08 00 05 64 2D 31 01 08 02 04 01 8E C3 C9 01 08 02 01 01 8F C3 C9 01 08 02 01 01 8C C3 C9 01 08 02 06 01 8D C3 C9 01 08 02 01 01 8A C3 C9 01 08 02 06 01 8B C3 C9 01 08 02 06 01 88 C3 C9 01 08 02 01 01 89 C3 C9 01 08 02 01 01 86 C3 C9 01 08 02 01 01 87 C3 C9 01 08 02 01 01 84 C3 C9 01 08 02 01 01 85 C3 C9 01 08 02 06 01 82 C3 C9 01 08 02 01 01 83 C3 C9 01 08 02 06 01 80 C3 C9 01 08 02 06 01 81 C3 C9 01 08 02 01 01 9F C3 C9 01 08 00 FF 9E C3 C9 01 08 00 FF 9D C3 C9 01 08 00 FF 9C C3 C9 01 08 00 FF 9B C3 C9 01 00 02 01 9A C3 C9 01 00 02 01 99 C3 C9 01 08 00 FF 98 C3 C9 01 08 00 FF 97 C3 C9 01 08 02 07 01 96 C3 C9 01 08 00 FF 95 C3 C9 01 08 00 FF 94 C3 C9 01 08 00 FF 93 C3 C9 01 08 02 05 01 92 C3 C9 01 08 02 01 01 91 C3 C9 01 08 02 05 01 90 C3 C9 01 08 02 01 01 AC C3 C9 01 08 04 08 02 AD C3 C9 01 08 00 08 AE C3 C9 01 08 00 FF AF C3 C9 01 08 00 FF A8 C3 C9 01 08 02 07 01 A9 C3 C9 01 08 02 07 01 AA C3 C9 01 08 00 FF AB C3 C9 01 08 00 09 A4 C3 C9 01 08 04 09 02 A5 C3 C9 01 08 04 09 02 A6 C3 C9 01 08 00 FF A7 C3 C9 01 08 00 FF A0 C3 C9 01 08 00 FF A1 C3 C9 01 08 00 09 A2 C3 C9 01 08 00 FF A3 C3 C9 01 08 00 FF B8 C3 C9 01 08 00 09 B5 C3 C9 01 08 00 FF B4 C3 C9 01 08 02 07 01 B7 C3 C9 01 08 00 0C B6 C3 C9 01 08 00 FF B1 C3 C9 01 08 06 08 01 02 B0 C3 C9 01 08 02 08 01 B3 C3 C9 01 08 00 FF B2 C3 C9 01 08 00 FF D6 FD FD 02 08 00 FF D7 FD FD 02 00 02 01 D4 FD FD 02 08 00 FF D5 FD FD 02 08 00 FF D2 FD FD 02 08 00 FF D3 FD FD 02 08 00 FF D0 FD FD 02 08 02 08 01 D1 FD FD 02 08 02 08 01 DC FD FD 02 08 00 FF DA FD FD 02 08 00 FF DB FD FD 02 08 00 09 D8 FD FD 02 00 02 01 D9 FD FD 02 08 00 FF C7 FD FD 02 08 02 01 01 C6 FD FD 02 08 02 01 01 C5 FD FD 02 08 02 01 01 C4 FD FD 02 08 02 01 01 C3 FD FD 02 08 02 01 01 C2 FD FD 02 08 02 01 01 C1 FD FD 02 08 02 01 01 C0 FD FD 02 08 02 01 01 CF FD FD 02 08 00 FF CE FD FD 02 08 00 FF CD FD FD 02 08 00 01 CC FD FD 02 08 02 07 01 CB FD FD 02 08 02 07 01 CA FD FD 02 08 02 01 01 C9 FD FD 02 08 02 02 01 C8 FD FD 02 08 02 01 01 4B 4A CB 01 08 00 09 4A 4A CB 01 08 00 09 49 4A CB 01 08 00 09 48 4A CB 01 08 02 09 01 4D 4A CB 01 08 00 FF 4C 4A CB 01 08 00 09 43 4A CB 01 08 02 08 01 42 4A CB 01 08 02 08 01 41 4A CB 01 00 06 01 02 40 4A CB 01 08 02 08 01 47 4A CB 01 08 02 08 01 46 4A CB 01 08 02 08 01 45 4A CB 01 08 02 08 01 44 4A CB 01 00 06 01 02 2D 4A CB 01 00 02 01 2C 4A CB 01 08 00 01 2F 4A CB 01 08 02 01 01 2E 4A CB 01 00 06 01 02 29 4A CB 01 08 02 01 01 28 4A CB 01 08 02 01 01 2B 4A CB 01 08 00 01 2A 4A CB 01 08 02 07 01 25 4A CB 01 08 02 07 01 24 4A CB 01 08 00 07 27 4A CB 01 08 02 07 01 26 4A CB 01 08 02 01 01 21 4A CB 01 08 02 01 01 20 4A CB 01 08 02 01 01 23 4A CB 01 08 00 07 22 4A CB 01 08 02 01 01 3C 4A CB 01 08 02 08 01 3D 4A CB 01 08 02 08 01 3E 4A CB 01 00 06 01 02 3F 4A CB 01 08 02 08 01 38 4A CB 01 08 00 07 39 4A CB 01 08 02 07 01 3A 4A CB 01 08 02 06 01 3B 4A CB 01 08 02 06 01 34 4A CB 01 08 00 01 35 4A CB 01 08 02 07 01 36 4A CB 01 08 00 07 37 4A CB 01 08 00 07 30 4A CB 01 08 02 07 01 31 4A CB 01 08 02 01 01 32 4A CB 01 08 02 01 01 33 4A CB 01 08 00 07 55 55 3A 01 08 02 08 01 54 55 3A 01 08 02 08 01 57 55 3A 01 08 02 07 01 56 55 3A 01 08 02 07 01 51 55 3A 01 08 02 08 01 50 55 3A 01 08 02 08 01 53 55 3A 01 08 02 08 01 52 55 3A 01 08 02 08 01 5D 55 3A 01 08 02 08 01 5C 55 3A 01 08 02 08 01 5F 55 3A 01 08 02 08 01 5E 55 3A 01 08 02 08 01 59 55 3A 01 08 00 FF 58 55 3A 01 08 02 08 01 5B 55 3A 01 08 00 FF 5A 55 3A 01 08 02 07 01 44 55 3A 01 08 00 FF 45 55 3A 01 08 00 FF 46 55 3A 01 08 02 08 01 47 55 3A 01 08 02 08 01 42 55 3A 01 08 02 06 01 43 55 3A 01 08 00 FF 4C 55 3A 01 08 00 FF 4D 55 3A 01 08 02 08 01 4E 55 3A 01 08 02 08 01 4F 55 3A 01 08 00 FF 48 55 3A 01 08 02 08 01 49 55 3A 01 08 02 08 01 4A 55 3A 01 08 02 08 01 4B 55 3A 01 08 02 08 01 66 55 3A 01 08 00 FF 67 55 3A 01 08 00 FF 65 55 3A 01 08 00 FF 62 55 3A 01 08 02 08 01 63 55 3A 01 00 06 01 02 60 55 3A 01 08 06 0C 01 02 61 55 3A 01 08 06 09 01 02 68 55 3A 01 08 00 FF 69 55 3A 01 08 00 FF 11 55 3A 01 08 00 FF 10 55 3A 01 08 00 FF 13 55 3A 01 08 00 FF 12 55 3A 01 08 00 FF 15 55 3A 01 08 00 FF 14 55 3A 01 08 00 FF 17 55 3A 01 08 00 FF 16 55 3A 01 08 00 FF 19 55 3A 01 08 00 FF 18 55 3A 01 08 00 FF 1B 55 3A 01 08 00 01 1A 55 3A 01 08 00 FF 1D 55 3A 01 08 02 05 01 1C 55 3A 01 08 00 01 1F 55 3A 01 08 00 FF 1E 55 3A 01 08 00 01 00 55 3A 01 08 00 05 01 55 3A 01 08 02 05 01 02 55 3A 01 08 02 05 01 03 55 3A 01 08 00 FF 04 55 3A 01 08 00 FF 05 55 3A 01 00 02 01 06 55 3A 01 08 02 08 01 07 55 3A 01 08 02 08 01 08 55 3A 01 08 02 05 01 09 55 3A 01 08 02 05 01 0A 55 3A 01 08 00 05 0B 55 3A 01 08 00 05 0C 55 3A 01 08 00 FF 0D 55 3A 01 08 00 FF 0E 55 3A 01 08 00 FF 0F 55 3A 01 08 00 FF 33 55 3A 01 08 02 06 01 32 55 3A 01 08 02 06 01 31 55 3A 01 08 02 06 01 30 55 3A 01 08 00 FF 37 55 3A 01 08 02 08 01 36 55 3A 01 08 02 07 01 35 55 3A 01 08 00 FF 34 55 3A 01 08 02 07 01 3B 55 3A 01 08 02 08 01 3A 55 3A 01 08 02 08 01 39 55 3A 01 08 02 08 01 38 55 3A 01 08 02 06 01 22 55 3A 01 08 02 05 01 23 55 3A 01 08 02 05 01 20 55 3A 01 08 00 06 21 55 3A 01 08 02 05 01 26 55 3A 01 08 02 05 01 27 55 3A 01 08 02 05 01 24 55 3A 01 08 02 05 01 25 55 3A 01 08 02 05 01 2A 55 3A 01 08 00 FF 2B 55 3A 01 08 00 FF 28 55 3A 01 08 02 05 01 29 55 3A 01 08 02 05 01 2E 55 3A 01 08 00 FF 2F 55 3A 01 08 00 FF 2C 55 3A 01 08 00 FF 2D 55 3A 01 08 00 FF CF 54 3A 01 08 02 05 01 CE 54 3A 01 08 02 05 01 CD 54 3A 01 08 02 05 01 CC 54 3A 01 08 02 05 01 CB 54 3A 01 08 02 06 01 CA 54 3A 01 08 00 05 C9 54 3A 01 08 02 05 01 C8 54 3A 01 08 02 05 01 C7 54 3A 01 08 00 01 C6 54 3A 01 08 00 01 C5 54 3A 01 08 00 01 C4 54 3A 01 08 00 01 C3 54 3A 01 08 00 01 C2 54 3A 01 08 02 05 01 C1 54 3A 01 08 00 01 C0 54 3A 01 08 00 01 DE 54 3A 01 08 02 05 01 DF 54 3A 01 08 02 05 01 DC 54 3A 01 08 00 FF DD 54 3A 01 08 02 05 01 DA 54 3A 01 08 02 05 01 DB 54 3A 01 08 00 FF D8 54 3A 01 08 02 05 01 D9 54 3A 01 08 00 01 D6 54 3A 01 08 02 05 01 D7 54 3A 01 08 02 05 01 D4 54 3A 01 08 02 05 01 D5 54 3A 01 08 02 05 01 D2 54 3A 01 08 02 05 01 D3 54 3A 01 08 02 05 01 D0 54 3A 01 08 02 05 01 D1 54 3A 01 08 02 06 01 ED 54 3A 01 08 02 05 01 EC 54 3A 01 08 02 05 01 EF 54 3A 01 08 02 05 01 EE 54 3A 01 08 02 05 01 E9 54 3A 01 08 00 FF E8 54 3A 01 08 00 FF EB 54 3A 01 08 02 05 01 EA 54 3A 01 08 02 05 01 E5 54 3A 01 08 02 01 01 E4 54 3A 01 08 02 05 01 E7 54 3A 01 08 02 01 01 E6 54 3A 01 08 02 01 01 E1 54 3A 01 08 02 05 01 E0 54 31 01 08 02 05 01 E3 54 3A 01 08 02 05 01 E2 54 3A 01 08 02 05 01 FC 54 3A 01 08 00 05 FD 54 3A 01 08 00 01 FE 54 3A 01 08 02 05 01 FF 54 3A 01 08 00 01 F8 54 3A 01 08 00 01 F9 54 3A 01 08 00 01 FA 54 3A 01 08 00 01 FB 54 3A 01 08 02 05 01 F4 54 3A 01 08 02 05 01 F5 54 3A 01 08 00 05 F6 54 3A 01 08 00 05 F7 54 3A 01 08 00 01 F0 54 3A 01 08 02 05 01 F1 54 3A 01 08 02 05 01 F2 54 3A 01 08 02 05 01 F3 54 3A 01 08 00 05 C8 D0 CC 01 00 02 01 C9 D0 CC 01 08 02 01 01 CA D0 CC 01 08 02 01 01 CB D0 CC 01 08 02 08 01 CC D0 CC 01 08 02 08 01 CD D0 CC 01 08 02 08 01 CE D0 CC 01 08 00 FF CF D0 CC 01 08 00 FF C0 D0 CC 01 08 02 08 01 C1 D0 CC 01 08 02 01 01 C2 D0 CC 01 08 02 01 01 C3 D0 CC 01 08 02 01 01 C4 D0 CC 01 08 02 01 01 C5 D0 CC 01 08 02 01 01 C6 D0 CC 01 08 02 01 01 C7 D0 CC 01 08 02 08 01 D9 D0 CC 01 08 02 09 01 D8 D0 CC 01 08 02 08 01 DB D0 CC 01 08 02 09 01 DA D0 CC 01 08 02 09 01 D1 D0 CC 01 08 02 08 01 D0 D0 CC 01 08 00 FF D3 D0 CC 01 08 02 08 01 D2 D0 CC 01 08 00 FF D5 D0 CC 01 08 02 08 01 D4 D0 CC 01 08 02 08 01 D7 D0 CC 01 08 02 08 01 D6 D0 CC 01 08 02 08 01 39 62 3D 01 08 00 FF 38 62 3D 01 08 00 FF 3B 62 3D 01 08 00 FF 3A 62 3D 01 08 00 FF 3D 62 3D 01 08 02 02 01 3C 62 3D 01 08 00 FF 3F 62 3D 01 08 02 02 01 3E 62 3D 01 08 02 04 01 31 62 3D 01 08 02 02 01 30 62 3D 01 08 02 02 01 33 62 3D 01 08 02 02 01 32 62 3D 01 08 02 02 01 35 62 3D 01 08 02 02 01 34 62 3D 01 08 02 02 01 37 62 3D 01 08 00 FF 36 62 3D 01 08 02 02 01 28 62 3D 01 08 02 02 01 29 62 3D 01 08 00 08 2A 62 3D 01 08 00 02 2B 62 3D 01 08 02 04 01 2C 62 3D 01 08 00 01 2D 62 3D 01 08 00 01 2E 62 3D 01 08 02 02 01 2F 62 3D 01 08 02 02 01 20 62 3D 01 08 02 02 01 21 62 3D 01 08 02 02 01 22 62 3D 01 08 02 02 01 23 62 3D 01 08 02 02 01 24 62 3D 01 08 02 02 01 25 62 3D 01 08 02 02 01 26 62 3D 01 08 00 07 27 62 3D 01 08 02 02 01 F7 F5 41 01 08 00 01 1B 62 3D 01 08 02 02 01 F6 F5 41 01 08 02 03 01 1A 62 3D 01 08 00 01 F5 F5 41 01 08 02 03 01 19 62 3D 01 08 02 02 01 F4 F5 41 01 08 02 03 01 18 62 3D 01 08 02 02 01 F3 F5 41 01 08 02 08 01 1F 62 3D 01 08 02 02 01 F2 F5 41 01 08 02 03 01 1E 62 3D 01 08 02 02 01 F1 F5 41 01 08 02 03 01 1D 62 3D 01 08 02 02 01 F0 F5 41 01 08 02 03 01 1C 62 3D 01 08 00 FF FF F5 41 01 08 02 04 01 13 62 3D 01 08 02 01 01 FE F5 41 01 08 02 04 01 12 62 3D 01 08 02 01 01 FD F5 41 01 08 00 FF 11 62 3D 01 08 02 01 01 FC F5 41 01 08 02 05 01 10 62 3D 01 08 02 01 01 FB F5 41 01 08 00 05 17 62 3D 01 08 02 02 01 FA F5 41 01 08 02 04 01 16 62 3D 01 08 02 02 01 F9 F5 41 01 08 02 04 01 15 62 3D 01 08 02 01 01 F8 F5 41 01 08 00 FF 14 62 3D 01 08 02 01 01 E6 F5 41 01 08 02 03 01 0A 62 3D 01 08 02 02 01 E7 F5 41 01 08 00 01 0B 62 3D 01 08 02 07 01 E4 F5 41 01 08 02 03 01 08 62 3D 01 08 02 02 01 E5 F5 41 01 08 02 03 01 09 62 3D 01 08 02 02 01 E2 F5 41 01 08 02 03 01 0E 62 3D 01 08 02 01 01 E3 F5 41 01 08 00 03 0F 62 3D 01 08 02 01 01 E0 F5 41 01 08 00 05 0C 62 3D 01 08 02 02 01 E1 F5 41 01 08 02 03 01 0D 62 3D 01 08 02 07 01 EE F5 41 01 08 00 03 02 62 3D 01 08 02 02 01 EF F5 41 01 08 00 03 03 62 3D 01 08 02 02 01 EC F5 41 01 08 02 04 01 00 62 3D 01 08 02 04 01 ED F5 41 01 00 02 01 01 62 3D 01 08 02 02 01 EA F5 41 01 08 00 FF 06 62 3D 01 08 02 02 01 EB F5 41 01 08 02 03 01 07 62 3D 01 08 02 02 01 E8 F5 41 01 08 02 03 01 04 62 3D 01 08 02 02 01 E9 F5 41 01 08 00 FF 05 62 3D 01 08 02 07 01 7D 62 3D 01 08 00 02 7C 62 3D 01 08 00 08 7F 62 3D 01 08 02 02 01 7E 62 3D 01 08 00 FF 79 62 3D 01 08 02 02 01 78 62 3D 01 08 02 02 01 7B 62 3D 01 08 02 05 01 7A 62 3D 01 08 02 04 01 75 62 3D 01 08 02 02 01 74 62 3D 01 08 00 02 77 62 3D 01 08 00 02 76 62 3D 01 08 06 08 01 02 71 62 3D 01 08 02 01 01 70 62 3D 01 08 00 01 73 62 3D 01 08 02 02 01 72 62 3D 01 08 02 01 01 6C 62 3D 01 08 02 02 01 6D 62 3D 01 08 02 01 01 6E 62 3D 01 08 02 01 01 6F 62 3D 01 08 02 01 01 68 62 3D 01 08 02 02 01 69 62 3D 01 08 00 02 6A 62 3D 01 08 02 02 01 6B 62 3D 01 08 00 02 64 62 3D 01 08 02 02 01 65 62 3D 01 08 02 02 01 66 62 3D 01 08 02 02 01 67 62 3D 01 08 02 04 01 60 62 3D 01 08 00 FF 61 62 3D 01 08 00 FF 62 62 3D 01 08 00 FF 63 62 3D 01 08 02 02 01 5F 62 3D 01 08 00 FF 5E 62 3D 01 08 00 FF 5D 62 3D 01 08 00 FF 5C 62 3D 01 08 00 FF 5B 62 3D 01 08 00 FF 5A 62 3D 01 08 02 02 01 59 62 3D 01 08 02 02 01 58 62 3D 01 08 02 02 01 57 62 3D 01 08 02 02 01 56 62 3D 01 08 02 02 01 55 62 3D 01 08 02 02 01 54 62 3D 01 08 02 02 01 53 62 3D 01 08 02 02 01 52 62 3D 01 08 00 01 51 62 3D 01 08 02 02 01 50 62 3D 01 08 02 04 01 4E 62 3D 01 08 02 02 01 4F 62 3D 01 08 02 02 01 4C 62 3D 01 08 00 02 4D 62 3D 01 08 02 02 01 4A 62 3D 01 08 02 02 01 4B 62 3D 01 08 02 02 01 48 62 3D 01 08 00 FF 49 62 3D 01 08 00 FF 46 62 3D 01 08 00 FF 47 62 3D 01 08 00 FF 44 62 3D 01 08 00 FF 45 62 3D 01 08 00 FF 42 62 3D 01 08 02 02 01 43 62 3D 01 08 02 04 01 40 62 3D 01 08 02 02 01 41 62 3D 01 08 02 02 01 B0 62 3D 01 08 02 05 01 B1 62 3D 01 08 02 03 01 B2 62 3D 01 08 02 04 01 B3 62 3D 01 08 02 04 01 B4 62 3D 01 08 00 FF B5 62 3D 01 08 00 FF B6 62 3D 01 08 02 05 01 B7 62 3D 01 08 02 05 01 B8 62 3D 01 08 02 08 01 B9 62 3D 01 08 02 06 01 BA 62 3D 01 08 02 05 01 BB 62 3D 01 08 02 04 01 BC 62 3D 01 08 02 06 01 BE 62 3D 01 08 02 07 01 BF 62 3D 01 08 02 06 01 A0 62 3D 01 08 00 02 A3 62 3D 01 08 02 07 01 A2 62 3D 01 08 02 05 01 A5 62 3D 01 08 02 03 01 A4 62 3D 01 08 00 FF A7 62 3D 01 08 00 FF A6 62 3D 01 08 00 FF A9 62 3D 01 08 06 08 01 02 A8 62 3D 01 08 00 FF AB 62 3D 01 08 02 03 01 AA 62 3D 01 08 02 03 01 AD 62 3D 01 08 02 03 01 AC 62 3D 01 08 00 08 AF 62 3D 01 08 00 08 AE 62 3D 01 08 00 FF 92 62 3D 01 08 02 03 01 93 62 3D 01 08 00 FF 90 62 3D 01 08 02 03 01 91 62 3D 01 08 00 FF 9E 62 3D 01 08 02 04 01 83 62 3D 01 08 02 07 01 82 62 3D 01 08 02 02 01 81 62 3D 01 08 00 02 80 62 3D 01 08 00 FF 87 62 3D 01 08 02 07 01 86 62 3D 01 08 02 07 01 85 62 3D 01 08 02 02 01 84 62 3D 01 08 02 07 01 8B 62 3D 01 08 00 FF 8A 62 3D 01 08 00 FF 89 62 3D 01 08 02 05 01 88 62 3D 01 08 00 07 8F 62 3D 01 08 02 02 01 8E 62 3D 01 08 02 03 01 8D 62 3D 01 08 02 03 01 8C 62 3D 01 08 02 03 01 E5 62 3D 01 08 06 09 01 00 E4 62 3D 01 08 00 FF E7 62 3D 01 08 04 09 02 E6 62 3D 01 08 00 FF E1 62 3D 01 08 00 09 E0 62 3D 01 08 02 08 01 E3 62 3D 01 08 00 FF E2 62 3D 01 08 00 09 E9 62 3D 01 08 00 FF E8 62 3D 01 08 00 FF EA 62 3D 01 08 02 08 01 D6 62 3D 01 08 02 07 01 D7 62 3D 01 08 02 07 01 D4 62 3D 01 08 02 07 01 D5 62 3D 01 08 00 07 D2 62 3D 01 08 06 07 01 02 D3 62 3D 01 08 02 08 01 D0 62 3D 01 08 06 08 01 02 D1 62 3D 01 08 02 07 01 DE 62 3D 01 08 06 09 01 00 DF 62 3D 01 08 00 FF DC 62 3D 01 08 06 07 01 02 DD 62 3D 01 08 00 FF DA 62 3D 01 08 02 07 01 DB 62 3D 01 08 00 FF D8 62 3D 01 08 02 07 01 D9 62 3D 01 08 00 08 C7 62 3D 01 08 02 05 01 C5 62 3D 01 08 00 FF C4 62 3D 01 08 02 05 01 C3 62 3D 01 08 06 07 01 02 C2 62 3D 01 08 02 08 01 C1 62 3D 01 08 00 FF C0 62 3D 01 08 02 08 01 CF 62 3D 01 08 02 07 01 CE 62 3D 01 08 02 06 01 CD 62 3D 01 08 02 06 01 CC 62 3D 01 08 02 06 01 CB 62 3D 01 08 02 07 01 CA 62 3D 01 08 02 06 01 C9 62 3D 01 00 06 01 02 C8 62 3D 01 08 02 07 01 2D F6 41 01 08 02 08 01 2C F6 41 01 08 02 08 01 2F F6 41 01 08 06 09 01 02 2E F6 41 01 08 02 08 01 29 F6 41 01 08 02 07 01 28 F6 41 01 08 02 06 01 2B F6 41 01 08 02 08 01 2A F6 41 01 08 00 FF 25 F6 41 01 08 02 08 01 24 F6 41 01 08 00 FF 27 F6 41 01 08 02 07 01 26 F6 41 01 08 02 06 01 21 F6 41 01 08 00 FF 20 F6 41 01 08 02 05 01 23 F6 41 01 08 02 06 01 22 F6 41 01 08 02 06 01 38 F6 41 01 08 00 FF 34 F6 41 01 08 00 09 35 F6 41 01 08 00 FF 36 F6 41 01 08 06 09 01 02 37 F6 41 01 08 00 FF 30 F6 41 01 08 00 FF 31 F6 41 01 08 04 09 02 32 F6 41 01 08 06 09 01 02 33 F6 41 01 08 00 09 0F F6 41 01 08 02 07 01 0E F6 41 01 08 02 07 01 0D F6 41 01 08 02 05 01 0C F6 41 01 08 02 05 01 0B F6 41 01 08 02 05 01 0A F6 41 01 08 02 05 01 09 F6 41 01 08 02 05 01 08 F6 41 01 08 02 05 01 07 F6 41 01 08 02 04 01 06 F6 41 01 08 02 04 01 05 F6 41 01 08 02 04 01 04 F6 41 01 08 02 04 01 03 F6 41 01 08 02 04 01 02 F6 41 01 08 02 04 01 01 F6 41 01 08 02 03 01 00 F6 41 01 08 02 04 01 1E F6 41 01 08 00 FF 1F F6 41 01 08 02 06 01 1C F6 41 01 08 02 06 01 1D F6 41 01 08 02 05 01 1A F6 41 01 08 02 08 01 1B F6 41 01 08 02 08 01 18 F6 41 01 08 00 FF 19 F6 41 01 08 02 06 01 16 F6 41 01 08 00 FF 17 F6 41 01 08 00 FF 14 F6 41 01 08 02 06 01 15 F6 41 01 08 02 08 01 12 F6 41 01 08 02 05 01 13 F6 41 01 08 02 06 01 10 F6 41 01 08 00 FF 11 F6 41 01 08 02 05 01 00 00 00 00 00 1F 00 0B 00 31 00 0A 06 70 02 14 00 40 E9 33 01 2E 00 32 00 33 00 2E 00 38 00 37 00 2E 00 32 00 30 00 38 00 00 00 0A 06 04 00 05 00 34 01 0C 06 0E 00 00 00 62 00 65 00 67 00 69 00 6E 00 49 00 70 00 00 00 00 00 00 00 09 00 09 00 28 00 10 06 C0 01 14 00 98 2F FA 02 64 00 49 00 70 00 00 00 00 00 17 00 2D 00 08 06 05 00 04 00 2C 00 0A 06 A0 01 14 00 01 00 00 00 00 00 00 00 07 0B 01 03 01 00 00 00 00 00 00 00 D8 84 FF 02 01 00 00 00 00 00 00 00 DB 84 FF 02 01 00 00 00 00 00 00 00 32 F6 41 01 01 00 00 00 00 00 00 00 DA 84 FF 02 01 00 00 00 01 00 00 00 07 0B 01 03 01 00 00 00 01 00 00 00 D8 84 FF 02 01 00 00 00 01 00 00 00 DB 84 FF 02 01 00 00 00 01 00 00 00 32 F6 41 01 01 00 00 00 01 00 00 00 DA 84 FF 02 02 00 00 00 00 00 00 00 07 0B 01 03 02 00 00 00 00 00 00 00 D8 84 FF 02 02 00 00 00 00 00 00 00 DB 84 FF 02 02 00 00 00 00 00 00 00 32 F6 41 01 02 00 00 00 00 00 00 00 DA 84 FF 02 02 00 00 00 01 00 00 00 07 0B 01 03 02 00 00 00 01 00 00 00 D8 84 FF 02 02 00 00 00 01 00 00 00 DB 84 FF 02 02 00 00 00 01 00 00 00 32 F6 41 01 02 00 00 00 01 00 00 00 DA 84 FF 02 03 00 00 00 00 00 00 00 07 0B 01 03 03 00 00 00 00 00 00 00 D8 84 FF 02 03 00 00 00 00 00 00 00 DB 84 FF 02 03 00 00 00 00 00 00 00 32 F6 41 01 03 00 00 00 00 00 00 00 DA 84 FF 02 03 00 00 00 01 00 00 00 07 0B 01 03 03 00 00 00 01 00 00 00 D8 84 FF 02 03 00 00 00 01 00 00 00 DB 84 FF 02 03 00 00 00 01 00 00 00 32 F6 41 01 03 00 00 00 01 00 00 00 DA 84 FF 02 04 00 00 00 00 00 00 00 07 0B 01 03 04 00 00 00 00 00 00 00 D8 84 FF 02 04 00 00 00 00 00 00 00 DB 84 FF 02 04 00 00 00 00 00 00 00 32 F6 41 01 04 00 00 00 00 00 00 00 DA 84 FF 02 04 00 00 00 01 00 00 00 07 0B 01 03 04 00 00 00 01 00 00 00 D8 84 FF 02 04 00 00 00 01 00 00 00 DB 84 FF 02 04 00 00 00 01 00 00 00 32 F6 41 01 04 00 00 00 01 00 00 00 DA 84 FF 02 05 00 00 00 00 00 00 00 07 0B 01 03 05 00 00 00 00 00 00 00 D8 84 FF 02 05 00 00 00 00 00 00 00 DB 84 FF 02 05 00 00 00 00 00 00 00 32 F6 41 01 05 00 00 00 00 00 00 00 DA 84 FF 02 05 00 00 00 01 00 00 00 07 0B 01 03 05 00 00 00 01 00 00 00 D8 84 FF 02 05 00 00 00 01 00 00 00 DB 84 FF 02 05 00 00 00 01 00 00 00 32 F6 41 01 05 00 00 00 01 00 00 00 DA 84 FF 02 06 00 00 00 00 00 00 00 07 0B 01 03 06 00 00 00 00 00 00 00 D8 84 FF 02 06 00 00 00 00 00 00 00 DB 84 FF 02 06 00 00 00 00 00 00 00 32 F6 41 01 06 00 00 00 00 00 00 00 DA 84 FF 02 06 00 00 00 01 00 00 00 07 0B 01 03 06 00 00 00 01 00 00 00 D8 84 FF 02 06 00 00 00 01 00 00 00 DB 84 FF 02 06 00 00 00 01 00 00 00 32 F6 41 01 06 00 00 00 01 00 00 00 DA 84 FF 02 07 00 00 00 00 00 00 00 07 0B 01 03 07 00 00 00 00 00 00 00 D8 84 FF 02 07 00 00 00 00 00 00 00 DB 84 FF 02 07 00 00 00 00 00 00 00 32 F6 41 01 07 00 00 00 00 00 00 00 DA 84 FF 02 07 00 00 00 01 00 00 00 07 0B 01 03 07 00 00 00 01 00 00 00 D8 84 FF 02 07 00 00 00 01 00 00 00 DB 84 FF 02 07 00 00 00 01 00 00 00 32 F6 41 01 07 00 00 00 01 00 00 00 DA 84 FF 02 08 00 00 00 00 00 00 00 07 0B 01 03 08 00 00 00 00 00 00 00 D8 84 FF 02 08 00 00 00 00 00 00 00 DB 84 FF 02 08 00 00 00 00 00 00 00 32 F6 41 01 08 00 00 00 00 00 00 00 DA 84 FF 02 08 00 00 00 01 00 00 00 07 0B 01 03 08 00 00 00 01 00 00 00 D8 84 FF 02 08 00 00 00 01 00 00 00 DB 84 FF 02 08 00 00 00 01 00 00 00 32 F6 41 01 08 00 00 00 01 00 00 00 DA 84 FF 02 00 00 20 00 75 71 0F 00 FB 1E 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 0E 8D 32 01 0E 8D 32 01 11 00 00 00 13 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 F9 D0 10 00 FC 1E 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 5E 8D 32 01 5E 8D 32 01 11 00 00 00 13 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 75 BF 0F 00 08 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 59 8D 32 01 59 8D 32 01 13 00 00 00 15 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 3D 5C 10 00 0B 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 0F 8D 32 01 0F 8D 32 01 11 00 00 00 13 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 AB E1 8A 00 27 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 10 8D 32 01 10 8D 32 01 14 00 00 00 16 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 BB E6 0F 00 29 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 5E 8D 32 01 5E 8D 32 01 14 00 00 00 16 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 66 BF 0F 00 2B 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 5E 8D 32 01 5E 8D 32 01 11 00 00 00 13 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 72 31 4F 00 39 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 11 8D 32 01 11 8D 32 01 14 00 00 00 16 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 AB 4C 0F 00 3C 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 60 8D 32 01 60 8D 32 01 13 00 00 00 15 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 F3 D0 10 00 49 1F 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 00 00 00 00 0F 00 00 00 12 8D 32 01 12 8D 32 01 14 00 00 00 16 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00");
    private static final byte[] warpCS = HexTool.getByteArrayFromHexString("00 00 00 63 00 74 00 65 00 64 00 2F 00 32 00 00 00 00 00 02 00 11 00 AD 01 08 06 02 00 00 00 33 00 00 00 05 00 13 00 AF 00 08 06 A0 01 14 00 30 E1 7B 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 18 00 B2 01 08 06 02 00 00 00 33 00 00 00 08 00 1A 00 B4 00 0A 06 B8 01 14 00 A8 10 88 06 69 00 6C 00 6C 00 2F 00 39 00 30 00 30 00 31 00");
    //private static final byte[] warpCS = HexTool.getByteArrayFromHexString("07 00 C2 53 10 00 01 00 62 FC 08 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 BE DB 3B 01 FF FF 07 00 AE 7E 10 00 01 00 62 54 0B 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 00 00 00 00 00 00 00 E4 63 3D 01 FF FF 07 00 88 F4 19 00 01 00 62 B4 14 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 00 00 00 00 00 00 00 8F F6 41 01 FF FF 07 00 E0 C8 10 00 01 00 62 EC 13 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 90 F6 41 01 FF FF 07 00 B5 D1 10 00 01 00 62 04 10 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 00 00 00 00 00 00 00 06 1E 2C 04 FF FF 07 00 10 E4 8A 00 01 00 63 48 3F 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 00 00 00 00 00 00 02 67 A1 98 00 68 A1 98 00 67 B5 C4 04 FF FF 07 00 0B 2E 1F 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 C1 8C 6C 05 80 00 00 00 40 42 0F 00 C1 8C 6C 05 80 00 00 00 20 A1 07 00 00 00 00 30 00 25 00 00 00 00 00 10 00 10 00 44 00 0A 06 98 BA 50 01 50 D3 B1 04 20 00 2D 00 20 00 34 00 34 00 20 00 2C 00 20 00 66 00 6F 00 72 00 20 00 35 00 39 00 73 00 65 00 63 00 73 00 2C 00 20 00 52 00 61 00 6E 00 67 00 65 00 20 00 32 00 30 00 30 00 25 00 00 00 00 00 03 00 0A 00 72 01 0C 06 06 00 00 00 68 00 32 00 39 00 00 00 00 00 00 00 03 00 0D 00");
    private static final byte[] CHAR_INFO_MAGIC = {-1, -55, -102, 59};

    //FIXED 商城道具
    public static MaplePacket warpCS(MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPEN.getValue());
        PacketHelper.addCharacterInfo(mplew, c.getPlayer());
        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.writeInt(0);
        final Collection<CashModInfo> cmi = c.getPlayer().iscs2 ? CashItemFactory.getInstance().getAllModInfo2() : CashItemFactory.getInstance().getAllModInfo();
        mplew.writeShort(cmi.size());
        if (ServerConstants.调试输出封包) {
            System.out.println("addModCashItemInfo--------------------");
        }
        for (CashModInfo cm : cmi) {
            addModCashItemInfo(mplew, cm);
        }
        mplew.writeShort(0);
        mplew.write(0);
        // 1080
        mplew.writeZeroBytes(120);
        int[] itemz = c.getPlayer().iscs2 ? CashItemFactory.getInstance().getBestItems2() : CashItemFactory.getInstance().getBestItems();
        for (int i = 1; i <= 8; i++) {
            for (int j = 0; j <= 1; j++) {
                for (int item = 0; item < itemz.length; item++) {
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(itemz[item]);
                }
            }
        }
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendBlockedMessage(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("sendBlockedMessage--------------------");
        }
        mplew.writeShort(SendPacketOpcode.BLOCK_MSG.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket playCashSong(int itemid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("playCashSong--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CASH_SONG.getValue());
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static MaplePacket show塔罗牌(String name, String otherName, int love, int cardId, int commentId) {
        if (ServerConstants.调试输出封包) {
            System.out.println("playCashSong--------------------");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_PREDICT_CARD.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(otherName);
        mplew.writeInt(love);
        mplew.writeInt(cardId);
        mplew.writeInt(commentId);
        //mplew.writeZeroBytes(3); 
        // mplew.writeInt(love);
        // mplew.writeInt(cardId);
        // mplew.writeInt(commentId);
        return mplew.getPacket();
    }

    public static MaplePacket useCharm(byte charmsleft, byte daysleft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("useCharm--------------------");
        }
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(6);
        mplew.write(1);
        mplew.write(charmsleft);
        mplew.write(daysleft);

        return mplew.getPacket();
    }

    public static MaplePacket useWheel(byte charmsleft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("useWheel--------------------");
        }
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(21);
        mplew.writeLong(charmsleft);

        return mplew.getPacket();
    }

    public static MaplePacket itemExpired(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("itemExpired--------------------");
        }
        // 1E 00 02 83 C9 51 00

        // 21 00 08 02
        // 50 62 25 00
        // 50 62 25 00
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(2);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static MaplePacket ViciousHammer(boolean start, int hammered) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("ViciousHammer--------------------");
        }
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        if (start) {
            mplew.write(49);
            mplew.writeInt(0);
            mplew.writeInt(hammered);
        } else {
            mplew.write(53);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static MaplePacket changePetFlag(int uniqueId, boolean added, int flagAdded) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("changePetFlag--------------------");
        }
        mplew.writeShort(SendPacketOpcode.PET_FLAG_CHANGE.getValue());

        mplew.writeLong(uniqueId);
        mplew.write(added ? 1 : 0);
        mplew.writeShort(flagAdded);

        return mplew.getPacket();
    }

    public static MaplePacket changePetName(MapleCharacter chr, String newname, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("changePetName--------------------");
        }
        mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());

        mplew.writeInt(chr.getId());
        mplew.write(0);
        mplew.writeMapleAsciiString(newname);
        mplew.write(slot);

        return mplew.getPacket();
    }

    public static MaplePacket showNotes(ResultSet notes, int count) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showNotes--------------------");
        }
        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from"));
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")));
            mplew.write(notes.getInt("gift"));
            notes.next();
        }

        return mplew.getPacket();
    }

    public static MaplePacket useChalkboard(final int charid, final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("useChalkboard--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());

        mplew.writeInt(charid);
        if (msg == null || msg.length() <= 0) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(msg);
        }

        return mplew.getPacket();
    }

    public static MaplePacket getTrockRefresh(MapleCharacter chr, boolean vip, boolean delete) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("getTrockRefresh--------------------");
        }
        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(delete ? 2 : 3);
        mplew.write(vip ? 1 : 0);
        if (vip) {
            int[] map = chr.getRocks();
            for (int i = 0; i < 10; i++) {
                mplew.writeInt(map[i]);
            }
        } else {
            int[] map = chr.getRegRocks();
            for (int i = 0; i < 5; i++) {
                mplew.writeInt(map[i]);
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendWishList(MapleCharacter chr, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("sendWishList--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(70);
        Connection con = DatabaseConnection.getConnection();
        int i = 10;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ? LIMIT 10");
            ps.setInt(1, chr.getAccountID());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.writeInt(rs.getInt("sn"));
                i--;
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            System.out.println("Error getting wishlist data:" + se);
        }
        while (i > 0) {
            mplew.writeInt(0);
            i--;
        }
        return mplew.getPacket();
    }

    public static MaplePacket showCashInventory(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(66);
        MapleCharacter cPlayer = c.getPlayer();
        if (ServerConstants.调试输出封包) {
            System.out.println("c.getPlayer() = " + cPlayer);
        }
        if (cPlayer != null) {
            CashShop mci = cPlayer.getCashInventory();
            if (ServerConstants.调试输出封包) {
                System.out.println("c.getPlayer().getCashInventory() = " + mci);
            }
            if (mci != null) {
                mplew.writeShort(mci.getItemsSize());
                if (ServerConstants.调试输出封包) {
                    System.out.println("addCashItemInfo--------------------");
                }
                for (IItem itemz : mci.getInventory()) {
                    addCashItemInfo(mplew, itemz, c.getAccID(), 0); //test
                }
            }
        }
        mplew.writeShort(cPlayer.getStorage().getSlots());
        mplew.writeShort(c.getCharacterSlots());
        return mplew.getPacket();
    }

    public static MaplePacket showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showNXMapleTokens--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
        mplew.writeInt(chr.getCSPoints(1)); // A-cash
        mplew.writeInt(chr.getCSPoints(2)); // MPoint

        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCSPackage(Map<Integer, IItem> ccc, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showBoughtCSPackage--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x7E); //use to be 7a
        mplew.write(ccc.size());
        for (Entry<Integer, IItem> sn : ccc.entrySet()) {
            addCashItemInfo(mplew, sn.getValue(), accid, sn.getKey());
        }
        mplew.writeShort(1);
        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCSItem(int itemid, int sn, int uniqueid, int accid, int quantity, String giftFrom, long expire) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showBoughtCSItemA--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(76); //use to be 4a
        addCashItemInfo(mplew, uniqueid, accid, itemid, sn, quantity, giftFrom, expire);

        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCSItem(IItem item, int sn, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showBoughtCSItemB--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(76);
        addCashItemInfo(mplew, item, accid, sn);

        return mplew.getPacket();
    }

    public static MaplePacket showXmasSurprise(int idFirst, IItem item, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showXmasSurprise--------------------");
        }
        mplew.writeShort(SendPacketOpcode.XMAS_SURPRISE.getValue());
        mplew.write(0xE6);
        mplew.writeLong(idFirst); //uniqueid of the xmas surprise itself
        mplew.writeInt(0);
        addCashItemInfo(mplew, item, accid, 0); //info of the new item, but packet shows 0 for sn?
        mplew.writeInt(item.getItemId());
        mplew.write(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, int accId, int sn) {
        addCashItemInfo(mplew, item, accId, sn, true);
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, int accId, int sn, boolean isFirst) {
        addCashItemInfo(mplew, item.getUniqueId(), accId, item.getItemId(), sn, item.getQuantity(), item.getGiftFrom(), item.getExpiration(), isFirst); //owner for the lulz
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire) {
        if (ServerConstants.调试输出封包) {
            System.out.println("addCashItemInfoC--------------------");
        }
        addCashItemInfo(mplew, uniqueid, accId, itemid, sn, quantity, sender, expire, true);
    }

    public static void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire, boolean isFirst) {
        if (ServerConstants.调试输出封包) {
            System.out.println("addCashItemInfoD--------------------");
        }
        mplew.writeLong(uniqueid > 0 ? uniqueid : 0);
        mplew.writeLong(accId);
        mplew.writeInt(itemid);
        mplew.writeInt(isFirst ? sn : 0);
        mplew.writeShort(quantity);
        mplew.writeAsciiString(sender, 13); //owner for the lulzlzlzl
        PacketHelper.addExpirationTime(mplew, expire);
        mplew.writeLong(0);
    }

    public static void addModCashItemInfo(MaplePacketLittleEndianWriter mplew, CashModInfo item) {
        int flags = item.flags;
        mplew.writeInt(item.sn);
        mplew.writeInt(flags);
        if ((flags & 0x1) != 0) {
            mplew.writeInt(item.itemid);
        }
        if ((flags & 0x2) != 0) {
            mplew.writeShort(item.count);
        }
        if ((flags & 0x4) != 0) {
            mplew.writeInt(item.discountPrice);
        }
        if ((flags & 0x8) != 0) {
            mplew.write(item.unk_1 - 1);
        }
        if ((flags & 0x10) != 0) {
            mplew.write(item.priority);
        }
        if ((flags & 0x20) != 0) {
            mplew.writeShort(item.period);
        }
        if ((flags & 0x40) != 0) {
            mplew.writeInt(0);
        }
        if ((flags & 0x80) != 0) {
            mplew.writeInt(item.meso);
        }
        if ((flags & 0x100) != 0) {
            mplew.write(item.unk_2 - 1);
        }
        if ((flags & 0x200) != 0) {
            mplew.write(item.gender);
        }
        if ((flags & 0x400) != 0) {
            mplew.write(item.showUp ? 1 : 0);
        }
        if ((flags & 0x800) != 0) {
            mplew.write(item.mark);
        }
        if ((flags & 0x1000) != 0) {
            mplew.write(item.unk_3 - 1);
        }
        if ((flags & 0x2000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x4000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x8000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x10000) != 0) {
            List<CashItemInfo> pack = CashItemFactory.getPackageItems(item.sn);
            if (pack == null) {
                mplew.write(0);
            } else {
                mplew.write(pack.size());
                for (int i = 0; i < pack.size(); i++) {
                    mplew.writeInt(pack.get(i).getSN());
                }
            }
        }
    }


    public static MaplePacket showBoughtCSQuestItem(int price, short quantity, byte position, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showBoughtCSQuestItem--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(111);
        mplew.writeInt(price);
        mplew.writeShort(quantity);
        mplew.writeShort(position);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static MaplePacket sendCSFail(int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("sendCSFail--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6A);
        mplew.write(err);

        return mplew.getPacket();
    }

    public static MaplePacket showCouponRedeemedItem(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showCouponRedeemedItemA--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(60);
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.writeShort(0x1A);
        mplew.writeInt(itemid);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket showCouponRedeemedItem(Map<Integer, IItem> items, int mesos, int maplePoints, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("showCouponRedeemedItemB--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(60); //use to be 4c
        mplew.write(items.size());
        for (Entry<Integer, IItem> item : items.entrySet()) {
            addCashItemInfo(mplew, item.getValue(), c.getAccID(), item.getKey().intValue());
        }
        mplew.writeLong(maplePoints);
        mplew.writeInt(mesos);

        return mplew.getPacket();
    }

    public static MaplePacket enableCSorMTS() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("enableCSorMTS--------------------");
        }
        mplew.write(HexTool.getByteArrayFromHexString("15 00 01 00 00 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("enableCSUse--------------------");
        }
        mplew.writeShort(18);
        mplew.writeInt(0);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static MaplePacket getCSInventory(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(66);
        CashShop mci = c.getPlayer().getCashInventory();
        mplew.writeShort(mci.getItemsSize());
        for (IItem itemz : mci.getInventory()) {
            Integer sn = null;
            try {
                sn = c.getPlayer().iscs2 ? CashItemFactory.getInstance().getSnFromId2(itemz.getItemId()) : CashItemFactory.getInstance().getSnFromId(itemz.getItemId());
            } catch (Exception ex) {
            }
            mplew.writeLong(itemz.getUniqueId());
            mplew.writeLong(c.getAccID());
            mplew.writeInt(itemz.getItemId());
            mplew.writeInt(sn == null ? 0 : sn);
            mplew.writeShort(itemz.getQuantity());
            mplew.writeAsciiString(itemz.getGiftFrom());
            for (int i = itemz.getGiftFrom().getBytes().length; i < 13; i++) {
                mplew.write(0);
            }
            PacketHelper.addExpirationTime(mplew, itemz.getExpiration());
            mplew.writeLong(0L);
        }
        mplew.writeShort(4);
        mplew.writeShort(3);
        return mplew.getPacket();
    }

    public static MaplePacket getCSGifts(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getCSGifts--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(68); //use to be 0x50 = 80
        System.out.println("c.getPlayer() = " + c.getPlayer());
        if (c.getPlayer() != null) {
            System.out.println("c.getPlayer().getCashInventory() = " + c.getPlayer().getCashInventory());
            if (c.getPlayer().getCashInventory() != null) {
                List<Pair<IItem, String>> mci = c.getPlayer().getCashInventory().loadGifts();
                if (mci != null) {
                    mplew.writeShort(mci.size());
                    for (Pair<IItem, String> mcz : mci) {
                        mplew.writeLong(mcz.getLeft().getUniqueId());
                        mplew.writeInt(mcz.getLeft().getItemId());
                        mplew.writeAsciiString(mcz.getLeft().getGiftFrom(), 13);
                        mplew.writeAsciiString(mcz.getRight(), 73);
                    }
                }
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket cashItemExpired(int uniqueid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("cashItemExpired--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(82); //use to be 5d
        mplew.writeLong(uniqueid);
        return mplew.getPacket();
    }

    public static MaplePacket sendGift(int itemid, int quantity, String receiver) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("sendGift--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(83); //use to be 7C
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(itemid);
        mplew.writeShort(quantity);

        return mplew.getPacket();
    }

    public static MaplePacket increasedInvSlots(int inv, int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("increasedInvSlots--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x65);
        mplew.write(inv);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    //also used for character slots !
    public static MaplePacket increasedStorageSlots(int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("increasedStorageSlots--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x67);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static MaplePacket confirmToCSInventory(IItem item, int accId, int sn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("confirmToCSInventory--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x5F);
        mplew.writeLong(item.getUniqueId());
        mplew.writeLong(accId);
        mplew.writeInt(item.getItemId());
        mplew.writeInt(sn);
        mplew.writeShort(item.getQuantity());
        mplew.writeAsciiString(item.getGiftFrom(), 13);
        //. mplew.writeAsciiString(item.getGiftFrom());
        // for (int i = item.getGiftFrom().getBytes().length; i < 13; i++) {
        //   mplew.write(0);
        // }
        PacketHelper.addExpirationTime(mplew, item.getExpiration());
        // mplew.writeLong(item.getExpire() == null ? DateUtil.getFileTimestamp(3439785600000L) : DateUtil.getFileTimestamp(item.getExpire().getTime()));
        mplew.writeLong(0L);
        //   addCashItemInfo(mplew, item, accId, sn, false);

        return mplew.getPacket();
    }

    public static MaplePacket confirmFromCSInventory(IItem item, short pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("confirmFromCSInventory--------------------");
        }
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x5D);
        mplew.writeShort(pos);
        PacketHelper.addItemInfo(mplew, item, true, true);

        return mplew.getPacket();
    }

    public static MaplePacket sendMesobagFailed() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("sendMesobagFailed--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MESOBAG_FAILURE.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket sendMesobagSuccess(int mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("sendMesobagSuccess--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MESOBAG_SUCCESS.getValue());
        mplew.writeInt(mesos);
        return mplew.getPacket();
    }

    //======================================MTS===========================================
    public static final MaplePacket startMTS(final MapleCharacter chr, MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("startMTS--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPEN.getValue());

        PacketHelper.addCharacterInfo(mplew, chr);

        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.writeInt(ServerConstants.MTS_MESO);
        mplew.writeInt(ServerConstants.MTS_TAX);
        mplew.writeInt(ServerConstants.MTS_BASE);
        mplew.writeInt(24);
        mplew.writeInt(168);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static final MaplePacket sendMTS(final List<MTSItemInfo> items, final int tab, final int type,
                                            final int page, final int pages) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("sendMTS--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x15); //operation
        mplew.writeInt(pages * 10); //total items
        mplew.writeInt(items.size()); //number of items on this page
        mplew.writeInt(tab);
        mplew.writeInt(type);
        mplew.writeInt(page);
        mplew.write(1);
        mplew.write(1);

        for (MTSItemInfo item : items) {
            addMTSItemInfo(mplew, item);
        }
        mplew.write(1); //0 or 1?

        return mplew.getPacket();
    }

    public static final MaplePacket showMTSCash(final MapleCharacter p) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("showMTSCash--------------------");
        }
        mplew.writeShort(SendPacketOpcode.GET_MTS_TOKENS.getValue());
//        mplew.writeInt(p.getCSPoints(1));
        mplew.writeInt(p.getCSPoints(2));
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSWantedListingOver(final int nx, final int items) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSWantedListingOver--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x3D);
        mplew.writeInt(nx);
        mplew.writeInt(items);
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSConfirmSell() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSConfirmSell--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x1D);
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSFailSell() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSFailSell--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x1E);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSConfirmBuy() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSConfirmBuy--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x33);
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSFailBuy() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSFailBuy--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x34);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSConfirmCancel() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSConfirmCancel--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x25);
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSFailCancel() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSFailCancel--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x26);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static final MaplePacket getMTSConfirmTransfer(final int quantity, final int pos) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("getMTSConfirmTransfer--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(quantity);
        mplew.writeInt(pos);
        return mplew.getPacket();
    }

    private static final void addMTSItemInfo(final MaplePacketLittleEndianWriter mplew, final MTSItemInfo item) {
        if (ServerConstants.调试输出封包) {
            System.out.println("addMTSItemInfo--------------------");
        }
        PacketHelper.addItemInfo(mplew, item.getItem(), true, true);
        mplew.writeInt(item.getId()); //id
        mplew.writeInt(item.getTaxes()); //this + below = price
        mplew.writeInt(item.getPrice()); //price
        mplew.writeInt(0);// Long?
        mplew.writeInt(KoreanDateUtil.getQuestTimestamp(item.getEndingDate()));
        mplew.writeInt(KoreanDateUtil.getQuestTimestamp(item.getEndingDate()));
        mplew.writeMapleAsciiString(item.getSeller()); //account name (what was nexon thinking?)
        mplew.writeMapleAsciiString(item.getSeller()); //char name
        mplew.writeZeroBytes(28);
    }

    public static final MaplePacket getNotYetSoldInv(final List<MTSItemInfo> items) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("getNotYetSoldInv--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x23);

        mplew.writeInt(items.size());

        for (MTSItemInfo item : items) {
            addMTSItemInfo(mplew, item);
        }

        return mplew.getPacket();
    }

    public static final MaplePacket getTransferInventory(final List<IItem> items, final boolean changed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("getTransferInventory--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x21);

        mplew.writeInt(items.size());
        int i = 0;
        for (IItem item : items) {
            PacketHelper.addItemInfo(mplew, item, true, true);
            mplew.writeInt(Integer.MAX_VALUE - i); //fake ID
            mplew.writeInt(110);
            mplew.writeInt(1011); //fake
            mplew.writeZeroBytes(48);
            i++;
        }
        mplew.writeInt(-47 + i - 1);
        mplew.write(changed ? 1 : 0);

        return mplew.getPacket();
    }

    public static final MaplePacket addToCartMessage(boolean fail, boolean remove) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (ServerConstants.调试输出封包) {
            System.out.println("addToCartMessage--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        if (remove) {
            if (fail) {
                mplew.write(0x2C);
                mplew.writeInt(-1);
            } else {
                mplew.write(0x2B);
            }
        } else if (fail) {
            mplew.write(0x2A);
            mplew.writeInt(-1);
        } else {
            mplew.write(0x29);
        }

        return mplew.getPacket();
    }

    public static MaplePacket sendHammerData(int hammerUsed, boolean type) { //金锤子
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("sendHammerData--------------------");
        }
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(type ? 0x53 : 0x54);
        mplew.writeInt(0);
        if (type) {
            mplew.writeInt(hammerUsed);
        }
        return mplew.getPacket();
    }

    public static MaplePacket hammerItem(IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (ServerConstants.调试输出封包) {
            System.out.println("hammerItem--------------------");
        }
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.write(3);
        mplew.write(1);
        mplew.write(item.getPosition());
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(item.getPosition());
        PacketHelper.addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    /*
     * 商城送礼物
     */
    public static final MaplePacket 商城送礼(int itemid, int quantity, String receiver) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        //mplew.write(CashShopOpcode.商城送礼.getValue());
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(itemid);
        mplew.writeShort(quantity);

        return mplew.getPacket();

    }
}
