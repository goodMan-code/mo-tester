package io.mo.util;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ScriptParser {
    private static String delimiter = COMMON.DEFAUT_DELIMITER;
    private static TestScript testScript = new TestScript();
    private static final Logger LOG = Logger.getLogger(ScriptParser.class.getName());

    public static void parseScript(String path){
        testScript = new TestScript();
        testScript.setFileName(path);
        int rowNum = 1;
        try {
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(path))));
            SqlCommand command = new SqlCommand();
            String line = lineReader.readLine();
            String trimmedLine;
            String issueNo = null;
            boolean ignore = false;
            int con_id = 0;

            while (line != null) {
                line = new String(line.getBytes(), StandardCharsets.UTF_8);
                trimmedLine = line.trim();
                //trimmedLine = line.replaceAll("\\s+$", "");

                //extract sql commands from the script file
                if (trimmedLine.equals("") || lineIsComment(trimmedLine)) {

                    //if line is  mark to relate to a bvt issue
                    //deal the tag bvt:issue:{issue number},when cases with this tag,will be ignored
                    if(trimmedLine.startsWith(COMMON.BVT_ISSUE_START_FLAG) && COMMON.IGNORE_MODEL) {
                        issueNo = trimmedLine.substring(COMMON.BVT_ISSUE_START_FLAG.length());
                        ignore = true;
                    }
                    if(trimmedLine.equalsIgnoreCase(COMMON.BVT_ISSUE_END_FLAG)) {
                        issueNo = null;
                        ignore = false;
                    }

                    //if line is mark to start a new connection
                    if(trimmedLine.startsWith(COMMON.NEW_SESSION_START_FLAG)){
                        if(!trimmedLine.contains("id=")){
                            LOG.warn("["+path+"][row:"+rowNum+"]The new connection flag doesn't specify the connection id by [id=X],and the id will be set to default value 1");
                            command.setConn_id(1);
                        }
                        con_id = Integer.parseInt(trimmedLine.substring(trimmedLine.indexOf("id=") + 3,trimmedLine.indexOf("id=") + 4));
                    }

                    if(trimmedLine.startsWith(COMMON.NEW_SESSION_END_FLAG)){
                        con_id = 0;
                    }

                    //if line is mark to set sort key index
                    if(trimmedLine.startsWith(COMMON.SORT_KEY_INDEX_FLAG)){
                        String[] indexes = trimmedLine.replaceAll(COMMON.SORT_KEY_INDEX_FLAG,"").split(",");
                        for (String index : indexes) {
                            command.addSortKeyIndex(Integer.parseInt(index));
                        }
                    }

                    //if line is mark to set column separator
                    if(trimmedLine.startsWith(COMMON.COLUMN_SEPARATOR_FLAG)){
                        String separator = trimmedLine.replaceAll(COMMON.COLUMN_SEPARATOR_FLAG,"");
                        command.setSeparator(separator);
                    }

                    line = lineReader.readLine();
                    rowNum++;
                    continue;
                }

                if(trimmedLine.contains(delimiter)){
                    command.append(trimmedLine);
                    command.setConn_id(con_id);
                    command.setIgnore(ignore);
                    command.setIssueNo(issueNo);
                    command.setPosition(rowNum);
                    testScript.addCommand(command);
                    command = new SqlCommand();
                }else {
                    command.append(trimmedLine);
                    command.append(COMMON.LINE_SEPARATOR);
                }
                line = lineReader.readLine();
                rowNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--") || trimmedLine.startsWith("#");
    }

    public static TestScript getTestScript(){
        return testScript;
    }

    public static void main(String[] args){
        
    }
}
