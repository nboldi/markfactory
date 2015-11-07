import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester extends hu.elte.markfactory.testbase.ReflectionTester {

	private static final String DO_NOT_TOUCH = "no-go";
	
	@ExamExercise
	public static void test() {
		
		try {
			String a = ((java.lang.String) staticFieldValue("Tested", "CONST"));
			String b = Tester.DO_NOT_TOUCH;
		} catch (hu.elte.markfactory.testbase.MissingProgramElementException e) {
			output(e);
			return;
		} catch (java.lang.Throwable e) {
			e.printStackTrace();
			return;
		}
	}
	
}
