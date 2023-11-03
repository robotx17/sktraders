package com.niruSoft.niruSoft.service;

import com.niruSoft.niruSoft.service.impl.GenerateBillImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;


@Slf4j
@Service
public class GenerateBillService implements GenerateBillImpl {


    @Override
    public JSONObject processExcelData(InputStream inputStream) {
        System.out.print("Going to Generate Patti's in ZIP");
        Map<String, List<Map<String, String>>> resultMap = new HashMap<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                System.out.println("No data or only header row found.");
                return new JSONObject(resultMap); // Convert resultMap to JSONObject and return it
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnIndexes = findColumnIndexes(headerRow);

            int farmerNameColumnIndex = columnIndexes.getOrDefault("FARMERNAME", -1);
            int itemQtyColumnIndex = columnIndexes.getOrDefault("ITEMQTY", -1);
            int itemColumnIndex = columnIndexes.getOrDefault("ITEM", -1);
            int unitColumnIndex = columnIndexes.getOrDefault("UNIT", -1);
            int rateColumnIndex = columnIndexes.getOrDefault("Rate", -1);

            // Loop through the rows and extract data for all farmer names
            for (int rowIndex = 1; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);

                // Add null check for dataRow
                if (dataRow != null) {
                    Cell farmerNameCell = dataRow.getCell(farmerNameColumnIndex);

                    if (farmerNameCell != null) {
                        String farmerName = getCellValueAsString(farmerNameCell, formulaEvaluator);
                        String itemQty = itemQtyColumnIndex != -1 ? getCellValueAsString(dataRow.getCell(itemQtyColumnIndex), formulaEvaluator) : "";
                        String item = itemColumnIndex != -1 ? getCellValueAsString(dataRow.getCell(itemColumnIndex), formulaEvaluator) : "";
                        String unit = unitColumnIndex != -1 ? getCellValueAsString(dataRow.getCell(unitColumnIndex), formulaEvaluator) : "";
                        String rate = rateColumnIndex != -1 ? getCellValueAsString(dataRow.getCell(rateColumnIndex), formulaEvaluator) : "";


                        Map<String, String> dataMap = new HashMap<>();
                        IntStream.range(0, headerRow.getPhysicalNumberOfCells()).forEach(cellIndex -> {
                            Cell dataCell = dataRow.getCell(cellIndex);
                            String cellValue = getCellValueAsString(dataCell, formulaEvaluator);
                            dataMap.put(headerRow.getCell(cellIndex).getStringCellValue(), cellValue);
                        });

                        resultMap.computeIfAbsent(farmerName, k -> new ArrayList<>()).add(dataMap);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Convert the original resultMap to a JSONObject
        JSONObject originalJson = new JSONObject(resultMap);

        //        System.out.print(modifiedJson);
        return modifyJsonStructure(originalJson); // Return the modified JSON
    }

    private JSONObject modifyJsonStructure(JSONObject originalJson) {
        JSONObject modifiedJson = new JSONObject();
        for (String farmerName : originalJson.keySet()) {
            JSONArray farmerDataArray = originalJson.getJSONArray(farmerName);
            JSONObject farmerDataObject = new JSONObject();

            // Initialize KGSUM and BAGSUM structures
            JSONObject kgSum = new JSONObject();
            JSONObject bagSum = new JSONObject();

            // Initialize SERIALITEM array to store ITEM values
            JSONArray serialItemArray = new JSONArray();

            StringBuilder particulersumBuilder = new StringBuilder();


            for (int i = 0; i < farmerDataArray.length(); i++) {
                JSONObject rowData = farmerDataArray.getJSONObject(i);

                String unit = rowData.optString("UNIT", "");
                String rate = rowData.optString("Rate", "");
                String qty = rowData.optString("QTY", "");
                String customerName = rowData.optString("CUSTOMER NAME", ""); // New line to get CUSTOMER NAME
                String itemQty = rowData.optString("Item qty", "");
                String item = rowData.optString("ITEM", "");

                if (!itemQty.isEmpty() && !item.isEmpty()) {
                    particulersumBuilder.append(itemQty).append(" ").append(item).append(", ");
                }
                // Check if the unit is KG's or BAG's
                if ("KG'S".equalsIgnoreCase(unit)) {
                    // Check if rate exists in KGSUM

                    if (isNumeric(rate) && Double.parseDouble(rate) == 0) {
                        // When Rate is 0, include CUSTOMER NAME in KGSUM instead of QTY
                        if (!kgSum.has("0")) {
                            kgSum.put("0", new JSONArray());
                        }
                        kgSum.getJSONArray("0").put(customerName);
                    } else if (!rate.isEmpty()) {
                        if (!kgSum.has(rate)) {
                            kgSum.put(rate, new JSONArray());
                        }
                        kgSum.getJSONArray(rate).put(qty);
                    }


                } else if ("BAG'S".equalsIgnoreCase(unit)) {
                    // Check if rate is numeric and equal to zero
                    if (isNumeric(rate) && Double.parseDouble(rate) == 0) {
                        // When Rate is 0, include CUSTOMER NAME in BAGSUM instead of QTY
                        if (!bagSum.has("0")) {
                            bagSum.put("0", new JSONArray());
                        }
                        bagSum.getJSONArray("0").put(customerName);
                    } else if (!rate.isEmpty()) {
                        if (!bagSum.has(rate)) {
                            bagSum.put(rate, new JSONArray());
                        }
                        bagSum.getJSONArray(rate).put(qty);
                    }
                }

                // Add ITEM value to SERIALITEM array
                serialItemArray.put(rowData.optString("ITEM", ", "));

                for (String header : rowData.keySet()) {
                    if (!farmerDataObject.has(header)) {
                        farmerDataObject.put(header, new JSONArray());
                    }
                    JSONArray headerArray = farmerDataObject.getJSONArray(header);
                    headerArray.put(rowData.getString(header));
                }
            }
            farmerDataObject.put("KGSUM", kgSum);
            farmerDataObject.put("BAGSUM", bagSum);
            farmerDataObject.put("SERIALITEM", serialItemArray);
            farmerDataObject.put("PARTICULERSUM", particulersumBuilder.toString().trim());

            modifiedJson.put(farmerName, farmerDataObject);
        }

        return modifiedJson;
    }


    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private Map<String, Integer> findColumnIndexes(Row headerRow) {
        Map<String, Integer> columnIndexes = new HashMap<>();
        for (int cellIndex = 0; cellIndex < headerRow.getPhysicalNumberOfCells(); cellIndex++) {
            String header = headerRow.getCell(cellIndex).getStringCellValue().replace(" ", "").toUpperCase();
            columnIndexes.put(header, cellIndex);
        }
        return columnIndexes;
    }


    private String getCellValueAsString(Cell cell, FormulaEvaluator formulaEvaluator) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // Use BigDecimal for numeric values
                        BigDecimal numericValue = new BigDecimal(cell.getNumericCellValue());
                        return numericValue.toString();
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    CellValue cellValue = formulaEvaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case STRING:
                            return cellValue.getStringValue();
                        case NUMERIC:
                            // Use BigDecimal for numeric values in formulas
                            BigDecimal formulaNumericValue = new BigDecimal(cellValue.getNumberValue());
                            return formulaNumericValue.toString();
                        case BOOLEAN:
                            return String.valueOf(cellValue.getBooleanValue());
                    }
                default:
                    return "";
            }
        }
        return "";
    }

    @Override
    public JSONObject processExcelFile(InputStream inputStream) {
        System.out.print("Going to Generate Slip in PDF ");
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, List<JSONObject>> dataMap = new LinkedHashMap<>();

            Iterator<Row> rowIterator = sheet.iterator();
            // Skip the header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String name = getCellValueAsString(row.getCell(2));

                JSONObject rowData = createJsonObjectFromRow(row);

                // Check if the name is already in the map
                if (dataMap.containsKey(name)) {
                    dataMap.get(name).add(rowData);
                } else {
                    List<JSONObject> dataList = new ArrayList<>();
                    dataList.add(rowData);
                    dataMap.put(name, dataList);
                }

                // Calculate and update "AMT" for each entry within the customer data
                int rate = rowData.getInt("Rate");
                int qty = rowData.getInt("QTY");
                if (rate != 0 && qty != 0) {
                    int amt = rate * qty;
                    rowData.put("AMT", amt);
                }
            }
            JSONObject resultJson = new JSONObject();
            for (Map.Entry<String, List<JSONObject>> entry : dataMap.entrySet()) {
                JSONArray dataArray = new JSONArray(entry.getValue());
                resultJson.put(entry.getKey(), dataArray);
            }

//            System.out.print(resultJson);
            return updateCountForDuplicates(resultJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private JSONObject createJsonObjectFromRow(Row row) {
        JSONObject jsonObject = new JSONObject();

        Cell dateCell = row.getCell(0);
        if (dateCell != null) {
            switch (dateCell.getCellType()) {
                case STRING:
                    jsonObject.put("DATE", dateCell.getStringCellValue());
                    break;
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(dateCell)) {
                        Date date = dateCell.getDateCellValue();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                        jsonObject.put("DATE", dateFormat.format(date));
                    } else {
                        jsonObject.put("DATE", String.valueOf(dateCell.getNumericCellValue()));
                    }
                    break;
                default:
                    // Handle other cell types as needed
                    jsonObject.put("DATE", "");
                    break;
            }
        } else {
            jsonObject.put("DATE", "");
        }

        jsonObject.put("ITEM", getCellValueAsString(row.getCell(1)));
        jsonObject.put("NAME", getCellValueAsString(row.getCell(2)));
        jsonObject.put("QTY", getNumericCellValue(row.getCell(3)));
        jsonObject.put("Rate", getNumericCellValue(row.getCell(4)));
        jsonObject.put("AMT", getNumericCellValue(row.getCell(5)));
        jsonObject.put("COUNT", getNumericCellValue(row.getCell(6)));

        return jsonObject;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (cell.getNumericCellValue() % 1 == 0) {
                        // Check if the numeric value has no decimal part
                        return String.valueOf((int) cell.getNumericCellValue());
                    } else {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                default:
                    return "";
            }

        }
        return "";
    }

    private double getNumericCellValue(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        return 0;
    }


    public static JSONObject updateCountForDuplicates(JSONObject resultJson) {
        for (String customerKey : resultJson.keySet()) {
            JSONArray customerTransactions = resultJson.getJSONArray(customerKey);
            Map<String, JSONObject> uniqueTransactions = new HashMap<>();

            for (int i = 0; i < customerTransactions.length(); i++) {
                JSONObject transaction = customerTransactions.getJSONObject(i);
                String transactionKey = transaction.toString();

                if (uniqueTransactions.containsKey(transactionKey)) {
                    // Update COUNT for duplicate transactions
                    int count = uniqueTransactions.get(transactionKey).getInt("COUNT") + 1;
                    uniqueTransactions.get(transactionKey).put("COUNT", count);
                } else {
                    // New unique transaction
                    uniqueTransactions.put(transactionKey, transaction);
                }
            }

            // Replace the original transactions with the updated unique transactions
            JSONArray updatedTransactions = new JSONArray(uniqueTransactions.values());
            resultJson.put(customerKey, updatedTransactions);
        }

//        System.out.println(resultJson);
        return multiplyCountByAmt(resultJson); // Return the modified JSON object
    }



    public static JSONObject multiplyCountByAmt(JSONObject updatedJson) {
        JSONObject multipliedJson = new JSONObject();

        for (String customerKey : updatedJson.keySet()) {
            JSONArray customerTransactions = updatedJson.getJSONArray(customerKey);
            JSONArray multipliedTransactions = new JSONArray();

            for (int i = 0; i < customerTransactions.length(); i++) {
                JSONObject transaction = customerTransactions.getJSONObject(i);
                int count = transaction.getInt("COUNT");
                int amt = transaction.getInt("AMT");
                int rate = transaction.getInt("Rate");
                int qty = transaction.getInt("QTY");

//                if (rate != 0 && qty != 0 ) {
//                    transaction.put("AMT", count * amt);
//                }
                if (rate != 0 && amt != 0) {
                    if (count == 0) {
                        transaction.put("AMT", rate * qty);
                    } else {
                        transaction.put("AMT", count * amt);
                    }
                }
                multipliedTransactions.put(transaction);
            }
            multipliedJson.put(customerKey, multipliedTransactions);
        }
        return combineData(multipliedJson);
    }

    public static JSONObject combineData(JSONObject updatedJson) {
        JSONObject combinedData = new JSONObject();

        for (String customerName : updatedJson.keySet()) {
            JSONArray transactions = updatedJson.getJSONArray(customerName);
            JSONObject combinedCustomerData = new JSONObject();

            for (int i = 0; i < transactions.length(); i++) {
                JSONObject transaction = transactions.getJSONObject(i);
                for (String key : transaction.keySet()) {
                    if (!combinedCustomerData.has(key)) {
                        JSONArray values = new JSONArray();
                        values.put(transaction.get(key));
                        combinedCustomerData.put(key, values);
                    } else {
                        JSONArray values = combinedCustomerData.getJSONArray(key);
                        values.put(transaction.get(key));
                    }
                }
            }

            combinedData.put(customerName, combinedCustomerData);
        }

        return combinedData;
    }


}

