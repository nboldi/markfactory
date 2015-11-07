import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester {
	
	private static final String DO_NOT_TOUCH = "no-go";

	@ExamExercise
	public static void test() {
		String a = Tested.CONST;
		String b = Tester.DO_NOT_TOUCH;
	}
	
}
