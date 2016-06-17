package me.onebone.economyproperty;

import cn.nukkit.scheduler.PluginTask;

public class AutoSaveTask extends PluginTask<EconomyProperty>{
	public AutoSaveTask(EconomyProperty plugin){
		super(plugin);
	}

	@Override
	public void onRun(int currentTick){
		this.getOwner().save();
	}
}
