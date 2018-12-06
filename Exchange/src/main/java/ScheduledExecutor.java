import org.zeromq.ZMQ;

import java.util.ArrayList;

public class ScheduledExecutor implements Runnable {

    private Company company;
    private ZMQ.Socket push;
    private ZMQ.Socket pub;

    public ScheduledExecutor(Company company, ZMQ.Socket push, ZMQ.Socket pub){
        this.company = company;
        this.push = push;
        this.pub = pub;
    }

    @Override
    public void run() {
        Auction auction = null;
        Emission emission = null;
        try{
            if((auction = company.getActiveAuction()) != null){
                finishAuction(auction);
                company.setActiveAuction(null);
            }else{
                emission = company.getActiveEmission();
                finishEmission(emission);
                company.setActiveEmission(null);
            }
        } catch (Exception e) {
            e.printStackTrace(); //never going to happen
        }

    }

    //-----------------------------------------------------------------------------------------------------------------------------

    private void finishAuction(Auction auction) {
        Pair<ArrayList<Pair<String,Long>>,ArrayList<String>> winnersAndLosers = auction.getWinnersLosers();

        ArrayList<Pair<String,Long>> winners = winnersAndLosers.getFirst();
        ArrayList<String> losers = winnersAndLosers.getSecond();

        //enviar mensagem aos vencedores
        if(winners != null){
            for(Pair winnerVal : winners){
                Protos.MessageWrapper msg = createAuctionWinningResultMsg(winnerVal);
                push.send(msg.toByteArray());
            }
            float max_rate = auction.getMaxBidRate(winners.size());
            this.company.setEmissionRate(max_rate);
        }
        //enviar mensagem aos perdedores
        if(losers != null){
            for(String investor : losers){
                Protos.MessageWrapper msg = createAuctionLoserResultMsg(investor);
                push.send(msg.toByteArray());
            }
        }
        //enviar mensagem à empresa
        Protos.MessageWrapper msgEmpresa = null;
        String notification = null;
        if(winners != null){
            String status = "Sucesso";
            msgEmpresa = createAuctionCompanyResultMsg(status);

            notification = this.company.getName() + ": Leilão Terminado, status: Sucesso, montante: " + auction.getValue();
        }else{
            String status = "Insucesso";
            msgEmpresa = createAuctionCompanyResultMsg(status);

            notification = this.company.getName() + ": Leilão Terminado, status: Insucesso, montante: " + auction.getValue();
        }
        push.send(msgEmpresa.toByteArray());
        pub.send(notification);
        //TODO: comunicar ao diretório
    }

    private Protos.MessageWrapper createAuctionWinningResultMsg(Pair winnerVal) {
        String client = (String) winnerVal.getFirst();
        Long value = (Long) winnerVal.getSecond();
        String msg = "Leilão terminado! Empresa: " + company.getName() + ", Status: Vencedor, Montante: " + value;

        return createAuctionEmissionResult(client, msg);
    }

    private Protos.MessageWrapper createAuctionLoserResultMsg(String investor) {

        String msg = "Leilão terminado! Empresa: " + company.getName() + ", Status: Perdedor";

        return createAuctionEmissionResult(investor, msg);
    }

    private Protos.MessageWrapper createAuctionCompanyResultMsg(String status) {
        String msg = "Leilão terminado! Status: " + status;
        String client = company.getName();

        return createAuctionEmissionResult(client, msg);

    }

    //-----------------------------------------------------------------------------------------------------------------------------

    private void finishEmission(Emission emission) {
        Pair<ArrayList<Pair<String,Long>>,ArrayList<String>> winnersAndLosers = emission.getWinnersLosers();

        ArrayList<Pair<String,Long>> winners = winnersAndLosers.getFirst();
        ArrayList<String> losers = winnersAndLosers.getSecond();

        long sum = 0;

        //enviar mensagem aos vencedores
        if(winners != null){
            for(Pair winnerVal : winners){
                sum += (long) winnerVal.getSecond();
                Protos.MessageWrapper msg = createEmissionWinningResultMsg(winnerVal);
                push.send(msg.toByteArray());
            }
        }
        //enviar mensagem aos perdedores
        if(losers != null){
            for(String investor : losers){
                Protos.MessageWrapper msg = createEmissionLoserResultMsg(investor);
                push.send(msg.toByteArray());
            }
            float nextEmissionRate = this.company.getEmissionRate() * (float) 1.1;
            this.company.setEmissionRate(nextEmissionRate);
        }
        //enviar mensagem à empresa
        Protos.MessageWrapper msgEmpresa = null;
        String notification = null;
        if(sum == emission.getValue()){
            String status = "Total";
            msgEmpresa = createEmissionCompanyResultMsg(status, emission.getValue(), sum);

            notification = this.company.getName() + ": Emissão Terminada, subscrição: Total, montante: " + emission.getValue();
        }else if(sum == 0){
            String status = "Nula";
            msgEmpresa = createEmissionCompanyResultMsg(status, emission.getValue(), sum);

            notification = this.company.getName() + ": Leilão Terminado, subscrição: Nula, montante: " + emission.getValue();
        }else{
            String status = "Parcial";
            msgEmpresa = createEmissionCompanyResultMsg(status, emission.getValue(), sum);

            notification = this.company.getName() + ": Leilão Terminado, subscrição: Sucesso, montante: " + emission.getValue();
        }
        push.send(msgEmpresa.toByteArray());
        pub.send(notification);
        //TODO: comunicar ao diretório
    }

    private Protos.MessageWrapper createEmissionWinningResultMsg(Pair winnerVal) {
        String client = (String) winnerVal.getFirst();
        Long value = (Long) winnerVal.getSecond();
        String msg = "Emissão terminada! Empresa: " + company.getName() + ", Status: Vencedor, Montante: " + value;

        return createAuctionEmissionResult(client, msg);
    }

    private Protos.MessageWrapper createEmissionLoserResultMsg(String investor) {
        String msg = "Emissão terminada! Empresa: " + company.getName() + ", Status: Perdedor";

        return createAuctionEmissionResult(investor, msg);
    }

    private Protos.MessageWrapper createEmissionCompanyResultMsg(String status, long value, long sum) {
        String msg = "Emissão terminada! Subscrição: " + status + ", Esperado: " + value + ", Obtido: " + sum;
        String client = company.getName();

        return createAuctionEmissionResult(client, msg);
    }

    //-----------------------------------------------------------------------------------------------------------------------------

    private Protos.MessageWrapper createAuctionEmissionResult(String client, String msg) {
        Protos.AuctionEmissionResult result = Protos.AuctionEmissionResult.newBuilder()
                .setClient(client)
                .setMsg(msg)
                .build();

        return Protos.MessageWrapper.newBuilder()
                .setMsgType(Protos.MessageWrapper.MessageType.ASYNC)
                .setAuctionemissionresult(result)
                .build();
    }


}
