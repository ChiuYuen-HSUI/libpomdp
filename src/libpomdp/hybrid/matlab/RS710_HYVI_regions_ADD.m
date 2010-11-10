% --------------------------------------------------------------------------- %
% libpomdp
% ========
% File: .m
% Description: hybrid value iteration simulation for
% RockSample[7,8] using ADDs
% Copyright (c) 2010, Diego Maniloff
% W3: http://www.cs.uic.edu/~dmanilof
% --------------------------------------------------------------------------- %

%% preparation
clear all
clear java
clear java

% add dynamic classpath
javaaddpath '../../../../external/jmatharray.jar'
javaaddpath '../../../../external/symPerseusJava.jar'
javaaddpath '../../../../dist/libpomdp.jar'

% java imports
import symPerseusJava.*;
import libpomdp.general.java.*;
import libpomdp.online.java.*;
import libpomdp.offline.java.*;
import libpomdp.hybrid.java.*;
import libpomdp.problems.rocksample.*;

%% load problem
factoredProb = pomdpAdd  ('../../problems/rocksample/7-10/RockSample_7_10.SPUDD');

%% load pre-computed offline bounds
load '../../problems/rocksample/7-10/RockSample_7_10_blind_ADD.mat';
load '../../problems/rocksample/7-10/RockSample_7_10_qmdp_ADD.mat';

%% create heuristics
aems2h  = aems2(factoredProb);
dosih   = DOSI(factoredProb);

% rocksample parameters for the grapher
GRID_SIZE         = 7;
ROCK_POSITIONS    = [2 0; 0 1; 3 1; 6 3; 2 4; 3 4; 5 5; 1 6; 6 0; 6 6];
SARTING_POS       = [0,3]
drawer            = rocksampleGraph;
NUM_ROCKS         = size(ROCK_POSITIONS,1);

% general parameters

EXPANSION_RATE       = 30; % calculated from the online simulator, avg
EPSILON_ACT_TH       = 1e-3;
EPISODECOUNT         = 10;
MAXEPISODELENGTH     = 100;
TOTALRUNS            = 2^NUM_ROCKS;
EXPANSIONTIME        = 0.9;
BACKUPTIME           = 0.1;
TOTALPLANNINGTIME    = 1.0;  
USE_FACTORED_BELIEFS = 1;

logFilename = sprintf('simulation-logs/rocksample/RS710-HYVI-regions-ADD-%s.log', date);

% stats
cumR              = [];
all.avcumrews     = [];
all.avTs          = [];
all.avreusedTs    = [];
all.avbackuptimes = [];
all.avexps        = [];
all.avbackups     = [];
all.avfoundeopt   = [];


% print general config problem parameters
diary(logFilename);
fprintf(1, '+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n');
fprintf(1, 'libpomdp log - config parameters\n');
fprintf(1, '--------------------------------\n');
fprintf(1, 'TOTALRUNS            = %d\n', TOTALRUNS);
fprintf(1, 'EPISODECOUNT         = %d\n', EPISODECOUNT);
fprintf(1, 'MAXEPISODELENGTH     = %d\n', MAXEPISODELENGTH);
fprintf(1, 'EXPANSIONTIME        = %d\n', EXPANSIONTIME);
fprintf(1, 'BACKUPTIME           = %d\n', BACKUPTIME);
fprintf(1, 'EPSILON_ACT_TH       = %d\n', EPSILON_ACT_TH);
fprintf(1, 'USE_FACTORED_BELIEFS = %d\n', USE_FACTORED_BELIEFS);
fprintf(1, '+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n');

for run = 1:TOTALRUNS
    
    fprintf(1, '///////////////////////////// UPDATE RUN %d of %d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\n',...
        run, TOTALRUNS);
    
    % stats
    all.stats{run}.cumrews        = [];
    all.stats{run}.backups        = [];
    all.stats{run}.foundeopt      = [];
    all.stats{run}.meanT          = [];
    all.stats{run}.meanreusedT    = [];
    all.stats{run}.meanbackuptime = [];
    all.stats{run}.meanexps       = [];
    
    
    % start this run
    for ep = 1:EPISODECOUNT

        fprintf(1, '********************** EPISODE %d of %d *********************\n', ep, EPISODECOUNT);
        
        % are we approximating beliefs with the product of marginals?
        if USE_FACTORED_BELIEFS
          b_init    = javaArray('symPerseusJava.DD', 1);          
          b_init(1) = factoredProb.getInit().bAdd;
          b_init    = BelStateFactoredADD( ...
              OP.marginals(b_init,factoredProb.getstaIds(),factoredProb.getstaIdsPr()), ...
              factoredProb.getstaIds());
        else
          b_init    = factoredProb.getInit();
        end
        
        % re - initialize tree at starting belief with original bounds
        aoTree = [];
        aoTree = AndOrTreeUpdateAdd(factoredProb, aems2h, dosih, lBound, uBound);
        aoTree.init(b_init);
        rootNode = aoTree.getRoot();

        % starting state for this set of EPISODECOUNT episodes
        factoredS = [factoredProb.getstaIds()' ; ...
                     1 + SARTING_POS, 1 + bitget((run-1), NUM_ROCKS:-1:1)];
        
        % stats
        cumR = 0;
        bakC = 0;
        fndO = 0;
        all.stats{run}.ep{ep}.R          = [];
        all.stats{run}.ep{ep}.exps       = [];
        all.stats{run}.ep{ep}.T          = [];
        all.stats{run}.ep{ep}.reusedT    = [];
        all.stats{run}.ep{ep}.backuptime = [];
        
        for iter = 1:MAXEPISODELENGTH
            
            fprintf(1, '******************** INSTANCE %d ********************\n', iter);
            tc = cell(factoredProb.printS(factoredS));
            fprintf(1, 'Current world state is:         %s\n', tc{1});
            drawer.drawState(GRID_SIZE, ROCK_POSITIONS, factoredS);
            if rootNode.belief.getClass.toString == ...
                  'class libpomdp.general.java.BelStateFactoredADD'
              fprintf(1, 'Current belief agree prob:      %d\n', ...                       
                      OP.evalN(rootNode.belief.marginals, factoredS));
            else
              fprintf(1, 'Current belief agree prob:      %d\n', ... 
                      OP.eval(rootNode.belief.bAdd, factoredS));
            end
            fprintf(1, 'Current |T| is:                 %d\n', rootNode.subTreeSize);

            % reset expand counter
            expC = 0;
            
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
            % EXPANSIONS for t_exp start here:
            %
            % start stopwatch for expansions
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
         
            tic
            while toc < EXPANSIONTIME
                % expand best node
                aoTree.expand(rootNode.bStar);
                % update its ancestors
                aoTree.updateAncestors(rootNode.bStar);
                % expand counter
                expC = expC + 1;
            end
            
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
            % BACKUP decision comes here:
            %
            % will go with a first approximation, and assume that ALL nodes
            % supported by alpha-vec i will get improved if we have a candidate
            % node supported by that alpha-vec i whose I(b) > 0
            % with this, the comparison we make is of k = exp_rate * t_bak,
            % is  k > max_i { #supportList[i] * exists(bakCandidate[i] } ?
            % throughout, we are also assuming (to our disadvantage) that
            % the additional k expansions we can get in t_bak would all be of
            % nodes that we can reuse
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

            tic
            backedup = 0;
            % get the support list for the action we are about to output
            % obtain the best action for the root
            % remember that a's and o's in matlab should start from 1
            % need to do this a priori!! otherwise currentBestAction() could
            % change since its randomized
            a = aoTree.currentBestAction();
            a = a + 1;
            
            % sort the support list for action a
            [sortedSupportList, oid] = sort(aoTree.fringeSupportLists(a,:), 'descend');
            for salphaid=1:aoTree.getLB().getSize()
                % if there exists a candidate, with I(b) > 0, and its fellow
                % nodes outnumber the expansions that could be made, backup
                if (rootNode.children(a).bakHeuristicStar(oid(salphaid)) > 0 && ...
                        sortedSupportList(salphaid) > EXPANSION_RATE * BACKUPTIME)
                    % compute backup
                    tic
                    aoTree.backupLowerAtNode(...
                        rootNode.children(a).bakCandidate(oid(salphaid)));
                    all.stats{run}.ep{ep}.backuptime(end+1) = toc;
                    bakC = bakC + 1;
                    % break loop
                    backedup = 1;
                    break;
                end
            end
            falseheurtime = toc
            
%             if(aoTree.expectedReuseRatio() > REUSETHRESHOLD && ...
%                     rootNode.children(aoTree.currentBestAction() + 1).bakHeuristicStar > 0)
%                 % bakup at best node according to heuristic
%                 tic
%                 aoTree.backupLowerAtNode(...
%                     rootNode.children(aoTree.currentBestAction() + ...
%                                       1).bakCandidate);
%                 all.stats{run}.ep{ep}.backuptime(end+1) = toc;
%                 bakC = bakC + 1;
%             else
       
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
            % EXTRA expansions may happen here for t_bak
            %
            % if not, continue expanding for the remaining 0.1 secs
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  
        
            if (backedup == 0)
                tic
                while toc < BACKUPTIME - falseheurtime
                    % expand best node
                    aoTree.expand(rootNode.bStar);
                    % update its ancestors
                    aoTree.updateAncestors(rootNode.bStar);
                    % expand counter
                    expC = expC + 1;
                end
            end
            
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
            % at this point, TOTALPLANNINGTIME has elapsed
            %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  
            
            
            % check whether we found an e-optimal action, either in the
            % EXPANSIONTIME slot or in the TOTALPLANNINGTIME slot
            if (aoTree.actionIsEpsOptimal(a-1, EPSILON_ACT_TH)) % careful with action indexes
                fprintf(1, 'Achieved e-optimal action!\n');
                fndO = fndO + 1;
            end
            
            % obtain the best action for the root
            % remember that a's and o's in matlab should start from 1
            %a = aoTree.currentBestAction();
            %a = a + 1;
            
            % execute best action and receive new o
            restrictedT = OP.restrictN(factoredProb.T(a), factoredS);
            factoredS1  = OP.sampleMultinomial(restrictedT, factoredProb.getstaIdsPr());
            restrictedO = OP.restrictN(factoredProb.O(a), [factoredS, factoredS1]);
            factoredO   = OP.sampleMultinomial(restrictedO, factoredProb.getobsIdsPr());

            % save stats
            all.stats{run}.ep{ep}.exps(end+1)  = expC;
            all.stats{run}.ep{ep}.R   (end+1)  = OP.eval(factoredProb.R(a), factoredS);
            all.stats{run}.ep{ep}.T   (end+1)  = rootNode.subTreeSize;
            cumR = cumR + ...
                factoredProb.getGamma^(iter - 1) * all.stats{run}.ep{ep}.R(end);
            

            % output some stats
            fprintf(1, 'Expansion finished, # expands:  %d\n',   expC);
            % this will count an extra |A||O| nodes given the expansion of the root
            fprintf(1, '|T|:                            %d\n',   rootNode.subTreeSize);
            tc = cell(factoredProb.getactStr(a-1));
            fprintf(1, 'Outputting action:              %s\n',   tc{1});
            tc = cell(factoredProb.printO(factoredO));
            fprintf(1, 'Perceived observation:          %s\n',   tc{1});
            fprintf(1, 'Received reward:                %.2f\n', all.stats{run}.ep{ep}.R(end));
            fprintf(1, 'Cumulative reward:              %.2f\n', cumR);

            % check whether this episode ended (terminal state)
            if(factoredS1(2,1) == GRID_SIZE+1)
                fprintf(1, '==================== Episode ended at instance %d==================\n', iter);
                drawer.drawState(GRID_SIZE, ROCK_POSITIONS, factoredS1);
                break;
            end

            %     pause;

            % move the tree's root node - RE_INIT INSTEAD FOR NOW
            o = Common.sencode(factoredO(2,:), ...
                factoredProb.getnrObsV(), ...
                factoredProb.getobsArity());
            % aoTree.moveTree(rootNode.children(a).children(o)); 
            % create new tree, but keep new bounds
            aoTree = AndOrTreeUpdateAdd(factoredProb, ...
                     aems2h, ...
                     dosih, ...
                     aoTree.getLB, aoTree.getUB);
            % initialize at the new belief
            aoTree.init(rootNode.children(a).children(o).belief);
            % update reference to rootNode
            rootNode = aoTree.getRoot();

            fprintf(1, 'Tree moved, reused |T|:         %d\n', rootNode.subTreeSize);
            all.stats{run}.ep{ep}.reusedT(end+1)  = rootNode.subTreeSize;
            
            % iterate
            %b = b1;
            factoredS = factoredS1;
            factoredS = Config.primeVars(factoredS, -factoredProb.getnrTotV);

        end % time-steps loop

        all.stats{run}.cumrews       (end+1)   = cumR;
        all.stats{run}.backups       (end+1)   = bakC;
        all.stats{run}.foundeopt     (end+1)   = fndO;
        all.stats{run}.meanT         (end+1)   = mean(all.stats{run}.ep{ep}.T);
        all.stats{run}.meanreusedT   (end+1)   = mean(all.stats{run}.ep{ep}.reusedT);
        all.stats{run}.meanbackuptime(end+1)   = mean(all.stats{run}.ep{ep}.backuptime);
        all.stats{run}.meanexps      (end+1)   = mean(all.stats{run}.ep{ep}.exps);
        %pause
        
    end % episodes loop
    
    % average cum reward of this run of 20 episodes
    all.avcumrews    (end+1)   = mean(all.stats{run}.cumrews);
    all.avbackups    (end+1)   = mean(all.stats{run}.backups);
    all.avfoundeopt  (end+1)   = mean(all.stats{run}.foundeopt);
    all.avTs         (end+1)   = mean(all.stats{run}.meanT);
    all.avreusedTs   (end+1)   = mean(all.stats{run}.meanreusedT);
    all.avbackuptimes(end+1)   = mean(all.stats{run}.meanbackuptime);
    all.avexps       (end+1)   = mean(all.stats{run}.meanexps);
    
end % runs loop

% save statistics before quitting
statsFilename = ...
    sprintf('simulation-logs/rocksample/RS710-ALLSTATS-HYVI-regions-ADD-%s.mat', date);
save(statsFilename, 'all');