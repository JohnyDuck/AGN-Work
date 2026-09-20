const mineflayer=require('mineflayer'); const {Vec3}=require('vec3'); const fs=require('fs');
const bot=mineflayer.createBot({host:'127.0.0.1',port:25565,username:'TrapdoorTester',version:'1.21.4',auth:'offline'});
const wait=ms=>new Promise(r=>setTimeout(r,ms));
bot.on('messagestr',m=>console.log('CHAT',m));
bot.on('error',e=>{console.error(e);process.exit(1)});
bot.once('spawn',async()=>{
 try{
 await wait(600);bot.chat('/gamemode creative');bot.chat('/tp @s -4.5 66 3.5');await wait(700);
 const button=bot.blockAt(new Vec3(-5,66,3)); console.log('BUTTON',button?.name);
 await bot.activateBlock(button);const start=Date.now();
 bot.chat('/tp @s 0.5 66 0.5');bot.chat('/gamemode survival');
 const samples=[];
 for(let i=0;i<100;i++){
   const floor=[0,1].flatMap(x=>[0,1].map(z=>bot.blockAt(new Vec3(x,65,z))?.name));
   samples.push({ms:Date.now()-start,floor,y:bot.entity.position.y});await wait(100);
 }
 fs.writeFileSync(require('path').join(__dirname,'../tests/player-click.json'),JSON.stringify(samples,null,2));
 console.log('OPEN SEEN',samples.some(s=>s.floor.every(b=>b==='air')),'CLOSED AT END',samples.at(-1).floor,'MIN Y',Math.min(...samples.map(s=>s.y)));
 bot.quit();setTimeout(()=>process.exit(0),500);
 }catch(e){console.error(e);bot.quit();process.exit(1)}
});
