'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
function load(engine,manifest){
 for(const [file,hash] of Object.entries(manifest.hashes)){
  if(crypto.createHash('sha256').update(fs.readFileSync(path.join(engine,file))).digest('hex')!==hash)throw Error('O motor mudou; revisão das regras competitivas necessária.');
 }
 const {TeamValidator}=require(path.join(engine,'sim/team-validator'));
 const {Teams}=require(path.join(engine,'sim/teams'));
 const {Dex}=require(path.join(engine,'sim/dex'));
 return {TeamValidator,Teams,Dex};
}
function validate(api,manifest,tier,teams){
 const id=manifest.formats[tier];if(!id)throw Error('Tier inválida');
 if(!Array.isArray(teams)||teams.length!==2)throw Error('Duas equipes obrigatórias');
 const rules=[id,'Dynamax Clause'];
 const reports=teams.map(packed=>{
  const team=typeof packed==='string'?api.Teams.unpack(packed):structuredClone(packed);
  if(!team?.length||team.length>6) return ['Equipe deve ter entre 1 e 6 Pokémon.'];
  for(const set of team){
   if(!Number.isInteger(set.level)||set.level<1||set.level>100)return ['Nível inválido: permitido de 1 a 100.'];
   if(!Array.isArray(set.moves)||set.moves.length<1||set.moves.length>4)return ['Quantidade de golpes inválida.'];
   for(const value of Object.values(set.ivs||{}))if(!Number.isInteger(value)||value<0||value>31)return ['IV inválido: permitido de 0 a 31.'];
   for(const value of Object.values(set.evs||{}))if(!Number.isInteger(value)||value<0||value>252)return ['EV inválido: permitido de 0 a 252 por atributo.'];
   if(Object.values(set.evs||{}).reduce((a,b)=>a+b,0)>510)return ['Total de EVs acima de 510.'];
   if(set.nature&&!api.Dex.natures.get(set.nature).exists)return ['Nature inválida.'];
  }
  // Unknown addon forms/items/moves fail closed through the pinned Gen 9 validator.
  const problems=new api.TeamValidator(id+'@@@Dynamax Clause').validateTeam(team)||[];
  return problems.filter(p=>!p.includes('If this was intentional, add exactly 1')).slice(0,12);
 });
 return {version:manifest.version,ok:reports.every(r=>r.length===0),reports,rules};
}
module.exports={load,validate};
if(require.main===module){
 try{
  const input=JSON.parse(fs.readFileSync(0,'utf8'));const manifest=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
  const api=load(path.resolve(process.argv[2]),manifest);process.stdout.write(JSON.stringify(validate(api,manifest,input.tier,input.teams)));
 }catch(e){process.stdout.write(JSON.stringify({ok:false,error:String(e.message).slice(0,300)}));process.exitCode=1;}
}
