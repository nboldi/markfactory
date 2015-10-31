import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester {

	
	@ExamExercise
	public static void test(Tested tested) {
		int b = tested.a;
	}

}
