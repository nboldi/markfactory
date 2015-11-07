import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester {

	public static String NOT_THAT = "NOT_THAT";
	
	@ExamExercise
	public static void test() {
		Tested.VAR = "VAR2";
		Tester.NOT_THAT = "NOT_THAT";
	}
	
}
