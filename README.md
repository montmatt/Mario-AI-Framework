This is a forked repository of a Mario AI framework, created by amidos2006. The purpose of of this framework is for our own educational purposes only (for Prof. Gillian Smith's WPI CS/IMGD 4100 class "Artificial Intelligence for Interactive Media and Games") to learn about and experiment with AI agents and behavior. Please use this following link of the original creator's full description and how-to guide for the framework: https://github.com/amidos2006/Mario-AI-Framework.

<h3 id="features">Group Members</h3>

------
- Matthew Montero
- Nicky Giangregorio
------

We didn't get anyone outside help our group, nor did we reference external sources other than the information provided by the source code provided in the original repository.

<h3 id="use">How To Use</h3>

------
For a full detailed instruction guide, please refer to their readme in their repository. The following are instructions based on the original creator's how-to use guide:

Download this repo and run the [`PlayLevel.java`](https://github.com/amidos2006/Mario-AI-Framework/blob/master/src/PlayLevel.java) file. It will run our custom AI agent [`macky`](https://github.com/amidos2006/Mario-AI-Framework/tree/master/src/agents/macky) in Visual Studio Code (VS Code). The game will run for 100 seconds (in-game time) and with Mario playing on the [third level](https://github.com/amidos2006/Mario-AI-Framework/blob/master/levels/original/lvl-3.txt). To change the parameters of the game (such as the agent, level, or time), just change their values in the following code
```
printResults(game.runGame(new agents.robinBaumgarten.Agent(), getLevel("levels/original/lvl-1.txt"), 100, 0, true));
```
where:
- Agent is "... agent.\[agent name\].Agetn(), ..."
- Level is "... getLevel("levels/original/lvl-\[level number\].txt") ..."
- Time is the third parameter of the printResults function (by default, it is the value 100 if you download our repo)

To run the game in VS Code, select "Run" -> either "Start Debugging" or "Run Without Debugging" options in the drop-down menu near the top left of the VS Code interface.