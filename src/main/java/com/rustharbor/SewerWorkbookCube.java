package com.rustharbor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class SewerWorkbookCube {
    private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";

    private SewerWorkbookCube() {
    }

    static CubeData load(Path workbookPath) throws Exception {
        if (!Files.exists(workbookPath)) {
            throw new IOException("Workbook not found: " + workbookPath);
        }

        try (ZipFile zipFile = new ZipFile(workbookPath.toFile(), StandardCharsets.UTF_8)) {
            List<String> sharedStrings = readSharedStrings(zipFile);
            Document workbookDoc = readXml(zipFile, "xl/workbook.xml");
            Document workbookRelsDoc = readXml(zipFile, "xl/_rels/workbook.xml.rels");

            Map<String, String> ridToTarget = readWorkbookRelationships(workbookRelsDoc);
            List<SheetRef> sheets = readSheetRefs(workbookDoc);

            String[] sheetNames = new String[sheets.size()];
            String[][][] cube = new String[sheets.size()][][];

            for (int s = 0; s < sheets.size(); s++) {
                SheetRef sheet = sheets.get(s);
                sheetNames[s] = sheet.name();
                String target = ridToTarget.get(sheet.relationshipId());
                if (target == null) {
                    cube[s] = new String[0][0];
                    continue;
                }

                String entryName = target.startsWith("xl/") ? target : "xl/" + target;
                Document sheetDoc = readXml(zipFile, entryName);
                cube[s] = readSheetValues(sheetDoc, sharedStrings);
            }

            return new CubeData(sheetNames, cube);
        }
    }

    private static List<String> readSharedStrings(ZipFile zipFile) throws Exception {
        ZipEntry sharedStringsEntry = zipFile.getEntry("xl/sharedStrings.xml");
        if (sharedStringsEntry == null) {
            return List.of();
        }

        Document sharedStringsDoc = readXml(zipFile, "xl/sharedStrings.xml");
        List<String> values = new ArrayList<>();

        NodeList siNodes = sharedStringsDoc.getElementsByTagNameNS(MAIN_NS, "si");
        for (int i = 0; i < siNodes.getLength(); i++) {
            Element si = (Element) siNodes.item(i);
            NodeList tNodes = si.getElementsByTagNameNS(MAIN_NS, "t");
            StringBuilder text = new StringBuilder();
            for (int t = 0; t < tNodes.getLength(); t++) {
                text.append(tNodes.item(t).getTextContent());
            }
            values.add(text.toString());
        }

        return values;
    }

    private static Map<String, String> readWorkbookRelationships(Document relationshipsDoc) {
        Map<String, String> ridToTarget = new HashMap<>();
        NodeList nodes = relationshipsDoc.getElementsByTagNameNS(PKG_REL_NS, "Relationship");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element rel = (Element) nodes.item(i);
            ridToTarget.put(rel.getAttribute("Id"), rel.getAttribute("Target"));
        }
        return ridToTarget;
    }

    private static List<SheetRef> readSheetRefs(Document workbookDoc) {
        List<SheetRef> sheets = new ArrayList<>();
        NodeList nodes = workbookDoc.getElementsByTagNameNS(MAIN_NS, "sheet");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element sheet = (Element) nodes.item(i);
            sheets.add(new SheetRef(
                    sheet.getAttribute("name"),
                    sheet.getAttributeNS(REL_NS, "id")
            ));
        }
        return sheets;
    }

    private static String[][] readSheetValues(Document sheetDoc, List<String> sharedStrings) {
        NodeList rowNodes = sheetDoc.getElementsByTagNameNS(MAIN_NS, "row");
        int maxRow = 0;
        int maxCol = 0;

        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element row = (Element) rowNodes.item(i);
            int rowIndex = parseIntOrDefault(row.getAttribute("r"), i + 1) - 1;
            maxRow = Math.max(maxRow, rowIndex + 1);

            NodeList cellNodes = row.getElementsByTagNameNS(MAIN_NS, "c");
            for (int j = 0; j < cellNodes.getLength(); j++) {
                Element cell = (Element) cellNodes.item(j);
                int colIndex = getColumnIndexFromRef(cell.getAttribute("r"));
                maxCol = Math.max(maxCol, colIndex + 1);
            }
        }

        if (maxRow == 0 || maxCol == 0) {
            return new String[0][0];
        }

        String[][] values = new String[maxRow][maxCol];
        for (int r = 0; r < maxRow; r++) {
            for (int c = 0; c < maxCol; c++) {
                values[r][c] = "";
            }
        }

        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element row = (Element) rowNodes.item(i);
            int rowIndex = parseIntOrDefault(row.getAttribute("r"), i + 1) - 1;

            NodeList cellNodes = row.getElementsByTagNameNS(MAIN_NS, "c");
            for (int j = 0; j < cellNodes.getLength(); j++) {
                Element cell = (Element) cellNodes.item(j);
                int colIndex = getColumnIndexFromRef(cell.getAttribute("r"));
                if (rowIndex < 0 || rowIndex >= maxRow || colIndex < 0 || colIndex >= maxCol) {
                    continue;
                }
                values[rowIndex][colIndex] = resolveCellValue(cell, sharedStrings);
            }
        }

        return values;
    }

    private static String resolveCellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");

        if ("inlineStr".equals(type)) {
            NodeList textNodes = cell.getElementsByTagNameNS(MAIN_NS, "t");
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < textNodes.getLength(); i++) {
                text.append(textNodes.item(i).getTextContent());
            }
            return text.toString();
        }

        NodeList valueNodes = cell.getElementsByTagNameNS(MAIN_NS, "v");
        if (valueNodes.getLength() == 0) {
            return "";
        }

        String raw = valueNodes.item(0).getTextContent();
        if (raw == null) {
            return "";
        }

        if ("s".equals(type)) {
            int index = parseIntOrDefault(raw, -1);
            if (index >= 0 && index < sharedStrings.size()) {
                return sharedStrings.get(index);
            }
            return "";
        }

        return raw;
    }

    private static int getColumnIndexFromRef(String cellRef) {
        if (cellRef == null || cellRef.isEmpty()) {
            return 0;
        }

        int value = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char ch = cellRef.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                value = value * 26 + (ch - 'A' + 1);
            } else {
                break;
            }
        }
        return Math.max(0, value - 1);
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static Document readXml(ZipFile zipFile, String entryName) throws Exception {
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null) {
            throw new IOException("Missing XLSX entry: " + entryName);
        }
        byte[] bytes = zipFile.getInputStream(entry).readAllBytes();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    record CubeData(String[] sheetNames, String[][][] values) {
        int sheetCount() {
            return values.length;
        }

        int rowCount(int sheetIndex) {
            if (sheetIndex < 0 || sheetIndex >= values.length) {
                return 0;
            }
            return values[sheetIndex].length;
        }

        int columnCount(int sheetIndex) {
            if (sheetIndex < 0 || sheetIndex >= values.length || values[sheetIndex].length == 0) {
                return 0;
            }
            return values[sheetIndex][0].length;
        }
    }

    private record SheetRef(String name, String relationshipId) {
    }
}
