/*
 * Copyright (c) 2012-2013, Ing. Gabriel Barrera <gmbarrera@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above 
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES 
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR 
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES 
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN 
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF 
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ia.battle.camp;

import ia.exceptions.OutOfMapException;
import ia.exceptions.RuleException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class BattleField {

	private ArrayList<BattleFieldListener> listeners;

	private static BattleField instance = new BattleField();

	private ConfigurationManager configurationManager;

	private long tick;
	private boolean inFight;

	private WarriorManager wm1, wm2;
	private WarriorWrapper currentWarriorWrapper, warriorWrapper1,
			warriorWrapper2;
	private HashMap<Warrior, WarriorWrapper> warriors;

	private FieldCell[][] cells;

	private Random random = new Random();

	private BattleField() {

		listeners = new ArrayList<BattleFieldListener>();
		configurationManager = ConfigurationManager.getInstance();

		initCells();
	}

	public static BattleField getInstance() {
		return instance;
	}

	private void initCells() {
		int height = configurationManager.getMapHeight();
		int width = configurationManager.getMapWidth();

		cells = new FieldCell[width][height];

		SpecialItemFactory sif = new SpecialItemFactory();
		SpecialItem si;

		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++) {

				if (Math.abs(random.nextGaussian()) > 2.5)
					si = sif.getSpecialItem();
				else
					si = null;

				if (Math.abs(random.nextGaussian()) > 2.1)
					cells[i][j] = new FieldCell(FieldCellType.BLOCKED, i, j,
							null);
				else
					cells[i][j] = new FieldCell(FieldCellType.NORMAL, i, j, si);

			}
	}

	public FieldCell getFieldCell(int x, int y) throws OutOfMapException {

		if ((x >= cells.length) || (x < 0) || (y >= cells[0].length) || (y < 0))
			throw new OutOfMapException();

		return cells[x][y];
	}

	/**
	 * 
	 * @return
	 */
	public WarriorData getEnemyData() {

		WarriorData enemyData;

		if (currentWarriorWrapper == warriorWrapper1)
			enemyData = new WarriorData(warriorWrapper2.getWarrior()
					.getPosition(), warriorWrapper2.getWarrior().getHealth(),
					warriorWrapper2.getWarrior().getName(),
					isWarriorInRange(warriorWrapper2.getWarrior()));
		else
			enemyData = new WarriorData(warriorWrapper1.getWarrior()
					.getPosition(), warriorWrapper1.getWarrior().getHealth(),
					warriorWrapper1.getWarrior().getName(),
					isWarriorInRange(warriorWrapper1.getWarrior()));

		return enemyData;
	}

	/**
	 * Este metodo es para uso interno del framework. Su uso es ilegal.
	 * 
	 * @return
	 * @throws RuleException
	 */
	public ArrayList<Warrior> getWarriors() throws RuleException {

		ArrayList<Warrior> warriors = new ArrayList<>();

		warriors.add(warriorWrapper1.getWarrior());
		warriors.add(warriorWrapper2.getWarrior());

		return warriors;
	}

	/**
	 * Devuelve si el warrior esta en el rango de ataque del warrior actual.
	 * 
	 * @param warrior
	 * @return
	 */
	boolean isWarriorInRange(Warrior warrior) {
		int centerX = currentWarriorWrapper.getWarrior().getPosition().getX();
		int centerY = currentWarriorWrapper.getWarrior().getPosition().getY();

		int range = currentWarriorWrapper.getWarrior().getRange();

		int x = warrior.getPosition().getX();
		int y = warrior.getPosition().getY();

		if ((Math.pow(centerX - x, 2)) + (Math.pow(centerY - y, 2)) <= Math
				.pow(range, 2)) {
			return true;
		}

		return false;
	}

	public ArrayList<FieldCell> getSpecialItems() {
		ArrayList<FieldCell> items = new ArrayList<FieldCell>();

		for (int i = 0; i < configurationManager.getMapWidth(); i++)
			for (int j = 0; j < configurationManager.getMapHeight(); j++)
				if (cells[i][j].getSpecialItem() != null)
					items.add(cells[i][j]);

		return items;
	}

	public void addWarriorManager(WarriorManager wm) {

		if (wm1 == null)
			wm1 = wm;
		else
			wm2 = wm;
	}

	private FieldCell getFreeCell() {
		FieldCell fieldCell = null;

		while ((fieldCell = cells[random.nextInt(configurationManager
				.getMapWidth())][random.nextInt(configurationManager
				.getMapHeight())]).getFieldCellType() == FieldCellType.BLOCKED)
			;

		return fieldCell;
	}

	private WarriorWrapper requestNextWarrior(WarriorManager wm) {

		WarriorWrapper wwrapper = null;

		try {
			wwrapper = new WarriorWrapper(wm.getNewWarrior());

			if (wwrapper.getWarrior().getName() == null)
				wwrapper.getWarrior().setName("Sin Nombre " + wm.hashCode());

		} catch (Exception e1) {
			// TODO:
			e1.printStackTrace();
		}

		warriors.put(wwrapper.getWarrior(), wwrapper);

		try {
			wwrapper.getWarrior().setPosition(getFreeCell());

		} catch (RuleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return wwrapper;
	}

	public void fight() {

		tick = 0;
		inFight = true;

		int actionPerTurns = ConfigurationManager.getInstance()
				.getActionsPerTurn();

		warriors = new HashMap<Warrior, WarriorWrapper>();

		warriorWrapper1 = requestNextWarrior(wm1);
		warriorWrapper2 = requestNextWarrior(wm2);

		for (BattleFieldListener listener : listeners)
			listener.startFight();

		// Who start the fight
		if (random.nextInt(2) == 0) {
			currentWarriorWrapper = warriorWrapper1;
		} else {
			currentWarriorWrapper = warriorWrapper2;
		}

		Action currentWarriorAction = null;

		do {

			tick++;
			if (tick % 2 == 0) {
				currentWarriorWrapper = warriorWrapper1;
			} else {
				currentWarriorWrapper = warriorWrapper2;
			}

			if (currentWarriorWrapper.getWarrior().getHealth() > 0) {

				currentWarriorWrapper.startTurn();

				for (int i = 0; i < actionPerTurns; i++) {

					currentWarriorAction = currentWarriorWrapper.getWarrior()
							.playTurn(tick, i);

					if (currentWarriorAction instanceof Move) {
						executeMoveAction((Move) currentWarriorAction);
					} else if (currentWarriorAction instanceof Attack) {
						executeAttackAction((Attack) currentWarriorAction);
					} else if (currentWarriorAction instanceof Skip) {
						executeSkipAction();
					} else if (currentWarriorAction instanceof Suicide) {
						executeSuicideAction();
					}

					for (BattleFieldListener listener : listeners)
						listener.turnLapsed(tick, i);

					try {
						Thread.sleep(50);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
			} else {
				if (currentWarriorWrapper == warriorWrapper1)
					warriorWrapper1 = requestNextWarrior(wm1);
				else
					warriorWrapper2 = requestNextWarrior(wm2);
			}

			try {
				Thread.sleep(50);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			for (BattleFieldListener listener : listeners)
				inFight &= listener.continueFighting();

		} while (inFight);
	}

	private void executeSkipAction() {

	}

	private void executeSuicideAction() {
		// TODO: Implementar el suicidio
		
		
		

	}

	private void executeAttackAction(Attack attack) {
		Warrior attackedWarrior = findWarriorInMap(attack.getCellToAttack());

		if (attackedWarrior == null) {
			return;
		}

		if (!isWarriorInRange(attackedWarrior))
			return;

		float damage = currentWarriorWrapper.getWarrior().getStrength();
		damage *= random.nextFloat();

		float defense = attackedWarrior.getDefense();
		defense *= (random.nextFloat() / 0.75 + 0.25);

		damage -= defense;

		if (damage > 0) {
			warriors.get(attackedWarrior).reduceHealth((int) damage);
		}
	}

	private Warrior findWarriorInMap(FieldCell cellToAttack) {

		for (Warrior w : warriors.keySet()) {
			if (w.getPosition() == cellToAttack)
				return w;
		}

		return null;
	}

	public List<FieldCell> getAdjacentCells(FieldCell fieldCell) {
		ArrayList<FieldCell> adjCells = new ArrayList<FieldCell>();

		int x = fieldCell.getX();
		int y = fieldCell.getY();

		if (x < configurationManager.getMapWidth() - 1)
			adjCells.add(cells[x + 1][y]);

		if (y < configurationManager.getMapHeight() - 1)
			adjCells.add(cells[x][y + 1]);

		if (x > 0)
			adjCells.add(cells[x - 1][y]);

		if (y > 0)
			adjCells.add(cells[x][y - 1]);

		if (x > 0 && y > 0)
			adjCells.add(cells[x - 1][y - 1]);

		if (x < configurationManager.getMapWidth() - 1
				&& y < configurationManager.getMapHeight() - 1)
			adjCells.add(cells[x + 1][y + 1]);

		if (x > 0 && y < configurationManager.getMapHeight() - 1)
			adjCells.add(cells[x - 1][y + 1]);

		if (x < configurationManager.getMapWidth() - 1 && y > 0)
			adjCells.add(cells[x + 1][y - 1]);

		return adjCells;
	}

	private void executeMoveAction(Move action) {

		ArrayList<FieldCell> currentWarriorActionsMoveCells;
		currentWarriorActionsMoveCells = action.move();

		FieldCell previousCell = currentWarriorWrapper.getWarrior()
				.getPosition();

		for (FieldCell fieldCell : currentWarriorActionsMoveCells) {

			currentWarriorWrapper.doStep();

			if (currentWarriorWrapper.getSteps() > currentWarriorWrapper
					.getWarrior().getSpeed() / 5)
				return;

			if (!fieldCell.equals(previousCell)) {
				List<FieldCell> adj = getAdjacentCells(previousCell);
				if (!adj.contains(fieldCell)) {
					//TODO: El warrior debe perder
					System.err.println("Esta trampeando!!!! " + previousCell
							+ " -> " + fieldCell);
					break;
				}
			}

			previousCell = fieldCell;

			if (fieldCell.getSpecialItem() != null) {

				SpecialItem si = fieldCell.removeSpecialItem();
				if (currentWarriorWrapper.getWarrior().useSpecialItem()) {
					si.affectWarrior(currentWarriorWrapper);
				}
			}

			FieldCell nueva_pos = fieldCell;

			try {
				if (nueva_pos.getX() >= 0
						&& nueva_pos.getX() < ConfigurationManager
								.getInstance().getMapWidth()
						&& nueva_pos.getY() >= 0
						&& nueva_pos.getY() < ConfigurationManager
								.getInstance().getMapHeight()
						&& (getFieldCell(nueva_pos.getX(), nueva_pos.getY()))
								.getFieldCellType() == FieldCellType.NORMAL) {
					try {

						currentWarriorWrapper.getWarrior().setPosition(
								nueva_pos);

					} catch (RuleException e) {
						e.printStackTrace();
					}
				}
			} catch (OutOfMapException e) {
				e.printStackTrace();
			}
		}
	}

	Warrior getWarrior1() {
		return this.warriorWrapper1.getWarrior();
	}

	Warrior getWarrior2() {
		return this.warriorWrapper2.getWarrior();
	}

	public long getTick() {
		return this.tick;
	}

	public void showResult() {
		// TODO Auto-generated method stub
		System.out.println("Termino la batalla. " + tick);

	}

	public void addListener(BattleFieldListener listener) {
		listeners.add(listener);
	}
}