package server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Utilities {
    private static final String PATH_CLIENTS = "src/main/resources/ClientsList";
    private static final String PATH_RATES = "src/main/resources/ExchangeRates";

    public static synchronized void addNewClient(String str) {
        try (FileWriter writer = new FileWriter(PATH_CLIENTS, true)) {
            writer.write(str + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkLogin(String login) throws IOException {
        for (String s : Files.readAllLines(Paths.get(PATH_CLIENTS))) {
            if (s.equals(login)) return true;
        }
        return false;
    }

    public static synchronized void addNewCurrency(int id) {
        try (FileWriter writer = new FileWriter(PATH_RATES, true)) {
            writer.write(Integer.toString(id) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkCurrency(int id) throws IOException {
        for (String s : Files.readAllLines(Paths.get(PATH_RATES))) {
            String[] strings = s.split("\\s+");
            if (strings[0].equals(Integer.toString(id))) return true;
        }
        return false;
    }

    public static synchronized boolean addRate(int id, double rate) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(PATH_RATES));
        boolean isAdd = false;
        for (int i = 0; i < lines.size(); i++) {
            String[] strings = lines.get(i).split("\\s+");
            if (Integer.parseInt(strings[0]) == id) {
                StringBuilder stringBuilder = new StringBuilder();
                for (String str : strings) stringBuilder.append(str).append(" ");
                stringBuilder.append(String.format("%-1.4f", rate));
                lines.set(i, stringBuilder.toString());
                isAdd = true;
                break;
            }
        }
        Files.write(Paths.get(PATH_RATES), lines);
        return isAdd;
    }

    public static synchronized String getListRates() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : Files.readAllLines(Paths.get(PATH_RATES))) {
            stringBuilder.append(formatRate(s)).append("\n");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private static String formatRate(String rate) {
        String[] strings = rate.split("\\s+");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[").append(strings[0]).append("]:\t");
        if (strings.length > 1) {
            stringBuilder.append(strings[1]).append(", ");
            if (strings.length > 2) {
                for (int i = 2; i < strings.length; i++) {
                    double temp = Double.parseDouble(strings[i]);
                    double prev = Double.parseDouble(strings[i - 1]);
                    stringBuilder.append(String.format("%7.4f(%1.4f/%1.4f)", temp, temp - prev, temp / prev)).append(",");
                }
            }
        }
        stringBuilder.setLength(stringBuilder.length() - 2);
        return stringBuilder.toString();
    }

    public static synchronized boolean deleteCurrency(int id) throws IOException {
        boolean isDelete = false;
        List<String> lines = Files.readAllLines(Paths.get(PATH_RATES));
        for (int i = 0; i < lines.size(); i++) {
            String[] strings = lines.get(i).split("\\s+");
            if (Integer.parseInt(strings[0]) == id) {
                lines.remove(i);
                isDelete = true;
                break;
            }
        }
        Files.write(Paths.get(PATH_RATES), lines);
        return isDelete;
    }
}