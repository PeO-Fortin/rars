package rars.concolic;

public interface OptionsChecker {
    int checkOption(String[] args, int index);
}
