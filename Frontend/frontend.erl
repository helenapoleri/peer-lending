-module(frontend).
-export([server/1]).
-include("protos.hrl").
-include("erlzmq2/include/erlzmq.hrl").

server(Port) -> 
	login_manager:start(),
	{ok, LSock} = gen_tcp:listen(Port, [binary, {packet, 4}, {reuseaddr, true}, {active, false}]),
	acceptor(LSock).

acceptor(LSock) ->
	% Espero a conexão de um cliente
	{ok, Sock} = gen_tcp:accept(LSock),
	% Crio um novo processo que vai aceitar mais clientes
	spawn(fun() -> acceptor(LSock) end),
	authenticate(Sock).

company(Sock) ->
	true.

investor(Sock) ->
	CompExchangeMap = get_exchanges(),
	{SocketsPush, SocketPull} = create_investor_topology(CompExchangeMap),
	spawn(fun() -> exchange_listener(Sock, SocketPull) end),
	investor_listener(Sock, SocketsPush).

get_exchanges() ->
	CompExchangeMap = #{
	%	empresa -> exchange port
		"empA" => { 1241, 1251},
		"empB" => { 1242, 1252},
		"empC" => { 1243, 1253}
	},
	CompExchangeMap.

create_investor_topology(Map) ->
	% criação de um contexto
	{ok,Context} = erlzmq:context(),
	% função que para cada empresa adiciona ao acumulador 
	% um socket de conexão ao Pull da exchange
	FunPush = fun(EMP, {PULL, PUSH}, Acc) ->
		{ok, Sender} = erlzmq:socket(Context, push),
		ok = erlzmq:connect(Sender, "tcp://localhost:" ++ PULL),
		maps:put(EMP,Sender,Acc) end,
	% função que para cada empresa faz com que o Socket receiver
	% se conecte a mais uma Exchange (Atençao!! a alterar porque
	% haverão empresas mapeadas na mesma exchange)
	FunPull = fun(EMP, {PULL, PUSH}, Receiver) ->
		ok = erlzmq:connect(Receiver, "tcp://localhost:" ++ PUSH),
		Receiver end,
	% Criação do mapeamento empresa/socket push
	SocketsPush = maps:fold(FunPush, #{}, Map),
	% Criação de um socket conetado a todas as exchanges
	{ok ,Receiver} = erlzmq:socket(Context, pull),
	SocketPull = maps:fold(FunPull, Receiver, Map),

	{SocketsPush, SocketPull}.

exchange_listener(Sock, SocketPull) ->
	true.

investor_listener(Sock, SocketsPush) ->
	true.

authenticate(Sock) ->
	io:fwrite("Start Authentication ~n",[]),
	Bin = recv(Sock),
	io:fwrite("Received Authentication Msg ~n",[]),
	{'LoginReq', User, Pass} = protos:decode_msg(Bin, 'LoginReq'),
	io:fwrite("Decoded msg ~n",[]),
	case login_manager:login(User, Pass) of
		{ok, company} ->
			Resp = protos:encode_msg(#'LoginResp'{cType='COMPANY', status='SUCCESS'}),
			gen_tcp:send(Sock, Resp),
			company(Sock);
		{ok, investor} ->
			Resp = protos:encode_msg(#'LoginResp'{cType='INVESTOR', status='SUCCESS'}),
			gen_tcp:send(Sock, Resp),
			investor(Sock);
		invalid ->
			Resp = protos:encode_msg(#'LoginResp'{status='INVALID'}),
			gen_tcp:send(Sock, Resp),
			authenticate(Sock)
	end.

recv(Sock) ->
    case gen_tcp:recv(Sock, 0) of
        {ok, B} ->
            B;
        {error, closed} ->
            error
    end.

do_recv(Sock, Bs) ->
    case gen_tcp:recv(Sock, 0) of
        {ok, B} ->
            do_recv(Sock, [Bs, B]);
        {error, closed} ->
            {ok, list_to_binary(Bs)}
    end.