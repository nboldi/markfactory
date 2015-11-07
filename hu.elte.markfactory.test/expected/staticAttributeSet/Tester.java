import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester extends hu.elte.markfactory.testbase.ReflectionTester {

	public static String NOT_THAT = "NOT_THAT";
	
	@ExamExercise
	public static void test() {
		
		try {
			staticFieldSet("Tested", "VAR", "VAR2");
			Tester.NOT_THAT = "NOT_THAT";
		} catch (hu.elte.markfactory.testbase.MissingProgramElementException e) {
			output(e);
			return;
		} catch (java.lang.Throwable e) {
			e.printStackTrace();
			return;
		}
	}
	
}
