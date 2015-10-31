import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester {

	
	@ExamExercise
	public static void test(Tested tested) {
		tested.a = 3;
	}

}
