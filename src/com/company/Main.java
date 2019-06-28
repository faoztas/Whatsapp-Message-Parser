package com.company;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    /* splits the message to two parts. The first one is date and the second one is rest of it. */
    public static final String firstRegex = "(.*)\\s-\\s(.*)";

    /* rips it off as a two parts. The first one is the person who sent it and and the second one is the text part. */
    public static final String secondRegex = "(.*):\\s(.*)";

    public static List<Message> messageList;

    public static Map<String,Integer> inCounter;
    public static Map<String,Integer> outCounter;

    public static void main(String[] args) {

        messageList = new LinkedList<>();

        String filename = "files\\E2808EPhiSoftware_Staj_ile_WhatsApp_Sohbeti_1.txt";

        try(
                FileReader reader = new FileReader(filename);
                BufferedReader buffer = new BufferedReader(reader)
        ){

            String line;
            while((line = buffer.readLine()) != null) {
                messageClassifier(line);
            }

        } catch (FileNotFoundException e) {
            System.out.println("Wrong path!");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        /****************************** INTERPRETATION ********************************************/

        inCounter = new HashMap<>();
        outCounter = new HashMap<>();

        interpreter();

        System.out.println("****** Geliş Sayısı ******");
        inCounter.entrySet()
                .stream()
                .sorted((o1, o2) -> o1.getValue()<o2.getValue()?1:-1)
                .forEach(stringIntegerEntry -> System.out.println(stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue()));

        System.out.println("\n****** Çıkış Sayısı ******");
        outCounter.entrySet()
                .stream()
                .sorted( (o1, o2) -> o1.getValue()<o2.getValue()?1:-1)
                .forEach(stringIntegerEntry -> System.out.println(stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue()));

    }

    public static void messageClassifier(String message) throws ParseException {

        Pattern parser = Pattern.compile(firstRegex);
        Matcher matcher = parser.matcher(message);

        /* Checks whether it is a new message or just a rest of the previous one */
        if(!matcher.find()) {
            messageList.get(messageList.size()-1).addMore(message);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("d MM yyyy HH:mm");
        Date date = dateFormat.parse(matcher.group(1));

        String rest = matcher.group(2);

        parser = Pattern.compile(secondRegex);
        matcher = parser.matcher(rest);

        /* A message which is sent by a person */
        if(matcher.find()) {
            Message msg = new Message(date,matcher.group(1),matcher.group(2));
            messageList.add(msg);
        }

        /* A message which is sent by WhatsApp itself, a notification*/
        else {
            Message msg = new Message(date,"WhatsApp",rest);
            messageList.add(msg);
        }

    }

    public static void interpreter(){

        /**********************************************/

        String inRegex = "gel.*(m|k|z|ı|i)(\\Z|\\s)";
        Pattern parserIn = Pattern.compile(inRegex);

        String outRegex = "(ç|c)(ı|i)k.*(m|k|z|ı|i)(\\Z|\\s)";
        Pattern parserOut = Pattern.compile(outRegex);

        String alsoRegex = "(ben de)|(bende)|(\\+1)";
        Pattern parserAlso = Pattern.compile(alsoRegex);

        String multipleRegex = "\\((.*)\\)";
        Pattern parserMultiple = Pattern.compile(multipleRegex);

        /**********************************************/

        String currentDirection = null;

        Date date = messageList.get(0).getSentDate();

        LinkedList<String> inDayCounter = new LinkedList<>();
        LinkedList<String> outDayCounter = new LinkedList<>();

        Matcher matcher;

        for(int i=0;i<messageList.size();i++){
            // Jumps when it belongs to WhatsApp
            if(messageList.get(i).getAuthor().equals("WhatsApp")) continue;

            String text = messageList.get(i).getText().toLowerCase();
            // Control if the day is over so we can reset the day counters linkedlists and uptade the haspmaps above
            if(date.getDay()!= messageList.get(i).getSentDate().getDay()) {
                transfer(inDayCounter,"in");
                transfer(outDayCounter,"out");
                inDayCounter = new LinkedList<>();
                outDayCounter = new LinkedList<>();
                date = messageList.get(i).getSentDate();
            }

    /*********************** In Case ***************/
            if((matcher = parserIn.matcher(text)).find()){

                // 3rd person case
                if(matcher.group(1).equals("ı") || matcher.group(1).equals("i")){
                    String[] splits = text.split(" ");
                    /**********************************************/
                    LinkedList<String> finalInDayCounter = inDayCounter;
                    /**********************************************/
                    inCounter.keySet().forEach(s -> {
                        if(s.toLowerCase().contains(splits[0]) && !finalInDayCounter.contains(s)) finalInDayCounter.add(s);
                    });

                    currentDirection = "in";

                    continue;
                }

                // create for the first time to count it
                if(inDayCounter.contains(messageList.get(i).getAuthor())) continue;

                inDayCounter.add(messageList.get(i).getAuthor());

                currentDirection = "in";
            }

    /********************** Out Case ***************/
            else if((matcher = parserOut.matcher(text)).find()){

                // 3rd person case
                if(matcher.group(3).equals("ı") || matcher.group(1).equals("i")){
                    String[] splits = text.split(" ");
                    /**********************************************/
                    LinkedList<String> finalOutDayCounter = outDayCounter;
                    /**********************************************/
                    inCounter.keySet().forEach(s -> {
                        if(s.toLowerCase().contains(splits[0]) && !finalOutDayCounter.contains(s)) finalOutDayCounter.add(s);
                    });

                    currentDirection = "out";

                    continue;
                }

                // create for the first time to count it
                if(outDayCounter.contains(messageList.get(i).getAuthor())) continue;

                outDayCounter.add(messageList.get(i).getAuthor());

                currentDirection = "out";
            }

    /*********************** Participation Case ***************/
            else if(parserAlso.matcher(text).find()){

                if(currentDirection=="in"){
                    if(inDayCounter.contains(messageList.get(i).getAuthor())) continue;
                    inDayCounter.add(messageList.get(i).getAuthor());
                }

                else if(currentDirection=="out"){
                    if(outDayCounter.contains(messageList.get(i).getAuthor())) continue;
                    outDayCounter.add(messageList.get(i).getAuthor());
                }

            }

    /********************** Multiple Persons ***************/
            if((matcher = parserMultiple.matcher(text)).find()){
                String[] splits = matcher.group(1).split("(,|\\s)");
                if(splits.length==1) continue; // to get rid of other stuff which are not fit

                for(int j=0;j<splits.length;j++){
                    if(splits[j].equals("")) continue;

                    /**********************************************/
                    int finalJ = j;
                    String finalCurrentDirection = currentDirection;
                    LinkedList<String> finalInDayCounter = inDayCounter;
                    LinkedList<String> finalOutDayCounter = outDayCounter;
                    /**********************************************/
                    inCounter.keySet().forEach(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            if(s.toLowerCase().contains(splits[finalJ])){
                                if(finalCurrentDirection == "in" && !finalInDayCounter.contains(s)) finalInDayCounter.add(s);
                                else if(finalCurrentDirection == "out" && !finalOutDayCounter.contains(s)) finalOutDayCounter.add(s);
                            }
                        }
                    });

                }
            }

        }

        /*register the last day's logs of the chat*/
        transfer(inDayCounter,"in");
        transfer(outDayCounter,"out");

    }

    public static void transfer(LinkedList<String> counter,String direction){

        if(direction == "in"){
            for(int i=0; i<counter.size(); i++){

                if(!inCounter.containsKey(counter.get(i))) inCounter.put(counter.get(i),0);

                inCounter.put(counter.get(i),inCounter.get((counter.get(i)))+1);
            }
        }

        else if(direction == "out"){
            for(int i=0; i<counter.size(); i++){

                if(!outCounter.containsKey(counter.get(i))) outCounter.put(counter.get(i),0);

                outCounter.put(counter.get(i),outCounter.get((counter.get(i)))+1);
            }
        }

    }

}
