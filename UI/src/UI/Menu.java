package UI;

import java.lang.reflect.Method;


public class Menu {

    public static void printMenu() {

        StringBuilder menuString = new StringBuilder();

        menuString.append("1)  " + "Switch user" + System.lineSeparator());
        menuString.append("2)  " + "Import from XML" + System.lineSeparator());
        menuString.append("3)  " + "Switch repository" + System.lineSeparator());
        menuString.append("4)  " + "Show status" + System.lineSeparator());
        menuString.append("5)  " + "Commit" + System.lineSeparator());
        menuString.append("6)  " + "Show active branch history" + System.lineSeparator());
        menuString.append("7)  " + "Show current commit info" + System.lineSeparator());
        menuString.append("8)  " + "Show branches" + System.lineSeparator());
        menuString.append("9)  " + "Create new branch" + System.lineSeparator());//TODO ask for checkout
        menuString.append("10) " + "Delete Branch" + System.lineSeparator());
        menuString.append("11) " + "Checkout" + System.lineSeparator());
        menuString.append("12) " + "Create new Repository" + System.lineSeparator());
        menuString.append("13) " + "Set commit for active branch" + System.lineSeparator()); //TODO
        menuString.append("14) " + "Exit" + System.lineSeparator());


        System.out.println(menuString);
    }


}