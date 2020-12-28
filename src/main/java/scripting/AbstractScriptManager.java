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
package scripting;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import client.MapleClient;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.mina.core.IoUtil;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;

/**
 * @author Matze
 */
public abstract class AbstractScriptManager {

    private static final ScriptEngineManager sem = new ScriptEngineManager();

    protected Invocable getInvocable(String path, MapleClient c) {
        return getInvocable(path, c, false);
    }

    protected Invocable getInvocable(String path, MapleClient c, boolean npc) {
        InputStream fr = null;
        try {

            path = "scripts/" + path;
            ScriptEngine engine = null;

            if (c != null) {
                engine = c.getScriptEngine(path);
            }
            if (engine == null) {
                File scriptFile = new File(path);
                if (!scriptFile.exists()) {
                    return null;
                }
                engine = sem.getEngineByName("javascript");

                if (c != null) {
                    c.setScriptEngine(path, engine);
                }
                fr = new FileInputStream(scriptFile);
//                InputStreamReader reader = new InputStreamReader(fr, EncodingDetect.getJavaEncode(scriptFile));
//                BufferedReader bf = new BufferedReader(reader);
                //jdk8 添加 try{load("nashorn:mozilla_compat.js");}catch (e){}
                String encoding = EncodingDetect.getJavaEncode(scriptFile);
                List<String> readLines = IOUtils.readLines(fr, encoding);
                String scrhead = "try{load(\"nashorn:mozilla_compat.js\");}catch(e){};";
                StringBuffer buffer = new StringBuffer();
                buffer.append(scrhead);
                readLines.stream().forEach(s -> {
                    buffer.append("\r\n");
                    buffer.append(s);
                });
                engine.eval(buffer.toString());
            } else if (c != null && npc) {
                NPCScriptManager.getInstance().dispose(c);
                c.getSession().write(MaplePacketCreator.enableActions());
                //c.getPlayer().dropMessage(5, "你现在已经假死请使用@ea");
            }
            return (Invocable) engine;
        } catch (Exception e) {
            System.err.println("Error executing script. Path: " + path + "\nException " + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing script. Path: " + path + "\nException " + e);
            return null;
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
}
