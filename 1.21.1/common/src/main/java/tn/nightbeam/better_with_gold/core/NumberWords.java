package tn.nightbeam.better_with_gold.core;

/** Small numbers as words, for chronicle lines ("survived three boss fights"). */
public final class NumberWords {
	private static final String[] ONES = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
	private static final String[] TENS = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};

	private NumberWords() {
	}

	public static String of(int n) {
		if (n < 0) {
			return Integer.toString(n);
		}
		if (n < 20) {
			return ONES[n];
		}
		if (n < 100) {
			return TENS[n / 10] + (n % 10 == 0 ? "" : "-" + ONES[n % 10]);
		}
		return Integer.toString(n);
	}

	/** "one boss fight", "three boss fights". */
	public static String count(int n, String singular, String plural) {
		return of(n) + " " + (n == 1 ? singular : plural);
	}

	public static String ordinal(int n) {
		return switch (n) {
			case 1 -> "first";
			case 2 -> "second";
			case 3 -> "third";
			case 4 -> "fourth";
			case 5 -> "fifth";
			default -> n + "th";
		};
	}
}
