package ch.idsia.ai.agents.ai;

import ch.idsia.ai.agents.Agent;
import ch.idsia.mario.environments.Environment;

import java.util.Random;

/**
 * Created by adamgressen on 10/16/14.
 */
public class TuringAgent extends BasicAIAgent implements Agent
{
    public TuringAgent()
    {
        super("TuringAgent");
        reset();
    }

    private Random R = null;
    public void reset()
    {
        // Dummy reset, of course, but meet formalities!
        R = new Random();
    }

    public boolean[] getAction(Environment observation)
    {
        boolean[] ret = new boolean[Environment.numberOfButtons];

        for (int i = 0; i < Environment.numberOfButtons; ++i)
        {
            // random boolean
            boolean chooseAction = R.nextBoolean();

            // left key
            if (i == 0) {
                chooseAction = (chooseAction && R.nextBoolean()) ? R.nextBoolean() :  chooseAction;
            }

            // right key
            else if (i == 1) {
                chooseAction = R.nextBoolean();
            }

            // down key
            else if (i == 2) {
                chooseAction = false;
            }

            // jump
            else if (i == 3) {
                chooseAction = true;
            }

            // speed
            else if (i == 4) {
                chooseAction = true;
            }

            // else
            else {
                chooseAction = false;
            }

            ret[i] = chooseAction;

            /*
            // Here the RandomAgent is encouraged to move more often to the Right and make long Jumps.
            boolean toggleParticularAction = R.nextBoolean();
            toggleParticularAction = (i == 0 && toggleParticularAction && R.nextBoolean()) ? R.nextBoolean() :  toggleParticularAction;
            toggleParticularAction = (i == 1 || i > 3 && !toggleParticularAction ) ? R.nextBoolean() :  toggleParticularAction;
            toggleParticularAction = (i > 3 && !toggleParticularAction ) ? R.nextBoolean() :  toggleParticularAction;
//            toggleParticularAction = (i == 4 && !toggleParticularAction ) ? R.nextBoolean() :  toggleParticularAction;
            ret[i] = toggleParticularAction;*/
        }
        return ret;
    }
}
