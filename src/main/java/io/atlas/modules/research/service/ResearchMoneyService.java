package io.atlas.modules.research.service;
import net.minecraft.server.level.ServerPlayer;
import io.atlas.modules.research.repository.ResearchRepository.Payment;
import java.math.BigInteger;
import java.lang.reflect.Method;
public final class ResearchMoneyService {
 private Object invoke(String name,ServerPlayer player,Object... args)throws Exception{var type=Class.forName("fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt");for(var method:type.getMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length+1&&method.getParameterTypes()[0].isAssignableFrom(player.getClass())){Object[] values=new Object[args.length+1];values[0]=player;System.arraycopy(args,0,values,1,args.length);return method.invoke(null,values);}throw new IllegalStateException("CobbleDollars API unavailable");}
 public BigInteger balance(ServerPlayer player)throws Exception{Object value=invoke("getCobbleDollars",player);if(!(value instanceof BigInteger amount)||amount.signum()<0)throw new IllegalStateException("Invalid CobbleDollars balance");return amount;}
 public boolean pay(ServerPlayer player,Payment payment,boolean recovering)throws Exception{
  return move(player,payment.id(),payment.amount(),"atlasResearchPaid:",false,recovering);
 }
 public boolean move(ServerPlayer player,java.util.UUID id,long amount,String prefix,boolean debit,boolean recovering)throws Exception{
  if(amount<1)throw new IllegalArgumentException();
  String marker=prefix+id;boolean already=player.getTags().contains(marker);if(recovering&&!already)throw new IllegalStateException("UNCERTAIN_PAYMENT");
  if(!already){if(player.getTags().size()>=1000)throw new IllegalStateException("Payment receipt capacity reached");BigInteger current=balance(player);if(debit&&current.compareTo(BigInteger.valueOf(amount))<0)throw new IllegalStateException("INSUFFICIENT_FUNDS");BigInteger expected=debit?current.subtract(BigInteger.valueOf(amount)):current.add(BigInteger.valueOf(amount));invoke("setCobbleDollars",player,expected);if(!balance(player).equals(expected))throw new IllegalStateException("Credit verification failed");player.addTag(marker);invoke("updateCobbleDollarsAccount",player);}
  invoke("updateCobbleDollarsAccount",player);
  var extension=Class.forName("fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt");var account=(java.io.File)extension.getMethod("getAccountFile",java.util.UUID.class,net.minecraft.server.MinecraftServer.class).invoke(null,player.getUUID(),player.getServer());try(var reader=java.nio.file.Files.newBufferedReader(account.toPath())){var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();if(!json.get("cobbledollars").getAsBigInteger().equals(balance(player)))throw new IllegalStateException("Account durability verification failed");}
  ((io.atlas.mixin.PlayerListSaveInvoker)player.getServer().getPlayerList()).atlas$savePlayer(player);
  var file=player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID()+".dat");var disk=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.create(16777216));boolean receipt=false;for(var tag:disk.getList("Tags",8))if(tag.getAsString().equals(marker))receipt=true;if(!receipt||!new BigInteger(disk.getString("CobbleDollars")).equals(balance(player)))throw new IllegalStateException("Payment durability verification failed");return true;
 }
}
