import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester {
	
	@ExamExercise
	public static void test() {
		Tested.numeric += 10;
		Tested.numeric -= 10;
		Tested.numeric *= 10;
		Tested.numeric /= 10;
		Tested.numeric %= 10;
		Tested.numeric |= 10;
		Tested.numeric &= 10;
		Tested.numeric <<= 10;
		Tested.numeric >>= 10;
		Tested.numeric ^= 10;
		++Tested.numeric;
		--Tested.numeric;
		Tested.numeric++;
		Tested.numeric--;
	}
	
}
